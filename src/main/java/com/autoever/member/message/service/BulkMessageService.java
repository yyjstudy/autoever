package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.service.ExternalMessageService;
import com.autoever.member.message.result.MessageSendResult;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.queue.MessageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 대량 메시지 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkMessageService {
    
    private final AgeCalculationService ageCalculationService;
    private final UserQueryService userQueryService;
    private final BatchProcessingService batchProcessingService;
    private final ExternalMessageService externalMessageService;
    private final FallbackMessageService fallbackMessageService;
    private final MessageSendTracker messageSendTracker;
    private final StructuredMessageLogger structuredLogger;
    private final MessageQueueService messageQueueService;
    
    // 임시로 메모리에 작업 상태 저장 (실제로는 DB나 Redis 사용)
    private final ConcurrentHashMap<UUID, BulkMessageJobStatus> jobStatusMap = new ConcurrentHashMap<>();
    
    // 작업별 진행 상태 추적
    private final ConcurrentHashMap<UUID, JobProgressTracker> jobProgressMap = new ConcurrentHashMap<>();
    
    /**
     * 대량 메시지 발송 시작
     */
    public BulkMessageResponse sendBulkMessage(MessageSendDto request) {
        UUID jobId = UUID.randomUUID();
        AgeGroup ageGroup = request.getAgeGroupEnum();
        
        log.info("대량 메시지 발송 작업 시작 - jobId: {}, ageGroup: {}", 
                 jobId, request.ageGroup());
        
        // 큐 상태 확인 - 큐가 꽉 찬 경우 즉시 실패 응답 반환
        MessageQueueService.QueueStatus queueStatus = messageQueueService.getQueueStatus();
        if (queueStatus.isFull()) {
            log.error("큐가 가득참 - 대량 메시지 발송 작업 실패 - jobId: {}, 현재큐크기: {}, 최대큐크기: {}", 
                     jobId, queueStatus.getCurrentSize(), queueStatus.getMaxSize());
            
            // 실패 상태로 작업 등록
            BulkMessageJobStatus failedStatus = new BulkMessageJobStatus(
                jobId,
                BulkMessageResponse.JobStatus.FAILED,
                0, 0, 0, 0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Duration.ZERO,
                0.0
            );
            jobStatusMap.put(jobId, failedStatus);
            
            structuredLogger.logJobStart(jobId, request.ageGroup(), request.message(), 0);
            
            return BulkMessageResponse.queueFull(jobId);
        }
        
        // 실제 사용자 수 조회
        int totalUsers = userQueryService.countUsersByAgeGroup(ageGroup);
        
        // 구조화된 로그 기록 (실제 사용자 수 포함)
        structuredLogger.logJobStart(jobId, request.ageGroup(), request.message(), totalUsers);
        
        if (totalUsers == 0) {
            log.warn("해당 연령대에 사용자가 없습니다 - ageGroup: {}", ageGroup);
            return createEmptyResponse(jobId);
        }
        
        BulkMessageResponse response = BulkMessageResponse.inProgress(jobId, totalUsers);
        
        // 진행 상태 추적기 초기화
        JobProgressTracker tracker = new JobProgressTracker(jobId, totalUsers);
        jobProgressMap.put(jobId, tracker);
        
        // 작업 상태 초기화
        BulkMessageJobStatus initialStatus = new BulkMessageJobStatus(
            jobId,
            BulkMessageResponse.JobStatus.IN_PROGRESS,
            totalUsers,
            0, // processedUsers
            0, // successCount
            0, // failureCount
            response.startedAt(),
            null, // completedAt
            null, // duration
            0.0 // progressPercentage
        );
        
        jobStatusMap.put(jobId, initialStatus);
        
        // 비동기 발송 시작
        processMessageSendingAsync(jobId, ageGroup, request.message());
        
        return response;
    }
    
    
    /**
     * 비동기 메시지 발송 처리
     */
    @Async("messageTaskExecutor")
    public CompletableFuture<Void> processMessageSendingAsync(UUID jobId, AgeGroup ageGroup, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("비동기 메시지 발송 시작 - jobId: {}", jobId);
                updateJobStatus(jobId, BulkMessageResponse.JobStatus.PROCESSING_BATCH);
                
                JobProgressTracker tracker = jobProgressMap.get(jobId);
                if (tracker == null) {
                    log.error("작업 진행 추적기를 찾을 수 없습니다 - jobId: {}", jobId);
                    return;
                }
                
                // 배치 처리로 사용자 조회 및 메시지 발송
                batchProcessingService.processBatchWithCallback(
                    ageGroup,
                    users -> sendMessagesToUsers(jobId, users, message, tracker),
                    progress -> updateJobProgress(jobId, progress, tracker)
                );
                
                // 최종 완료 처리
                completeJob(jobId, tracker);
                
            } catch (Exception e) {
                log.error("메시지 발송 중 오류 발생 - jobId: {}", jobId, e);
                updateJobStatus(jobId, BulkMessageResponse.JobStatus.FAILED);
            }
        });
    }
    
    /**
     * 사용자 리스트에게 메시지 발송
     */
    private void sendMessagesToUsers(UUID jobId, List<User> users, String message, JobProgressTracker tracker) {
        long batchStartTime = System.currentTimeMillis();
        log.debug("배치 메시지 발송 - jobId: {}, userCount: {}", jobId, users.size());
        
        updateJobStatus(jobId, BulkMessageResponse.JobStatus.SENDING_MESSAGES);
        
        int batchSuccessCount = 0;
        int batchFailureCount = 0;
        
        for (User user : users) {
            long messageStartTime = System.currentTimeMillis();
            
            try {
                // FallbackMessageService를 통한 템플릿 적용 및 Fallback 발송
                MessageSendResult result = fallbackMessageService.sendWithFallback(user, message);
                
                long responseTime = System.currentTimeMillis() - messageStartTime;
                
                // 결과에 따른 분류 처리
                if (result == MessageSendResult.QUEUE_FULL) {
                    // 큐가 가득 찬 경우 즉시 실패 처리하고 작업 중단
                    tracker.incrementFailure();
                    batchFailureCount++;
                    
                    String errorMessage = "큐 용량 초과로 작업 중단: " + result.getDescription();
                    structuredLogger.logMessageFailure(jobId, user.getPhoneNumber(), errorMessage, responseTime);
                    
                    log.error("큐가 가득참 - 대량 발송 작업 중단 - userId: {}, phone: {}, responseTime: {}ms", 
                            user.getId(), maskPhoneNumber(user.getPhoneNumber()), responseTime);
                    
                    // 큐가 가득 찬 경우 더 이상 발송하지 않고 실패로 처리
                    updateJobStatus(jobId, BulkMessageResponse.JobStatus.FAILED);
                    throw new RuntimeException("큐 용량 초과로 인한 발송 실패");
                    
                } else if (result.isSuccess()) {
                    tracker.incrementSuccess();
                    batchSuccessCount++;
                    
                    log.trace("메시지 발송 성공 - userId: {}, phone: {}, result: {}, responseTime: {}ms", 
                             user.getId(), maskPhoneNumber(user.getPhoneNumber()), result, responseTime);
                    
                } else {
                    tracker.incrementFailure();
                    batchFailureCount++;
                    
                    // 구조화된 에러 로그
                    String errorMessage = "발송 실패: " + result.getDescription();
                    structuredLogger.logMessageFailure(jobId, user.getPhoneNumber(), errorMessage, responseTime);
                    
                    log.warn("메시지 발송 실패 - userId: {}, phone: {}, result: {}, responseTime: {}ms", 
                            user.getId(), maskPhoneNumber(user.getPhoneNumber()), result, responseTime);
                }
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - messageStartTime;
                tracker.incrementFailure();
                batchFailureCount++;
                
                // 구조화된 에러 로그
                structuredLogger.logMessageFailure(jobId, user.getPhoneNumber(), e.getMessage(), responseTime);
                
                log.warn("메시지 발송 중 예외 발생 - userId: {}, phone: {}, error: {}, responseTime: {}ms", 
                        user.getId(), maskPhoneNumber(user.getPhoneNumber()), e.getMessage(), responseTime);
            }
            
            tracker.incrementProcessed();
        }
        
        // 배치 처리 완료 로그
        long batchDuration = System.currentTimeMillis() - batchStartTime;
        structuredLogger.logBatchProcessing(jobId, tracker.getBatchNumber(), users.size(), 
            batchDuration, batchSuccessCount, batchFailureCount);
    }
    
    /**
     * 작업 진행 상황 업데이트
     */
    private void updateJobProgress(UUID jobId, BatchProcessingService.BatchProgress batchProgress, JobProgressTracker tracker) {
        BulkMessageJobStatus currentStatus = jobStatusMap.get(jobId);
        if (currentStatus == null) return;
        
        double progressPercentage = BulkMessageJobStatus.calculateProgress(
            tracker.getProcessedCount(), tracker.getTotalUsers());
        
        BulkMessageJobStatus updatedStatus = new BulkMessageJobStatus(
            jobId,
            currentStatus.status(),
            tracker.getTotalUsers(),
            tracker.getProcessedCount(),
            tracker.getSuccessCount(),
            tracker.getFailureCount(),
            currentStatus.startedAt(),
            currentStatus.completedAt(),
            currentStatus.duration(),
            progressPercentage
        );
        
        jobStatusMap.put(jobId, updatedStatus);
        
        log.debug("작업 진행 상황 업데이트 - jobId: {}, progress: {}%, processed: {}/{}", 
                 jobId, progressPercentage, tracker.getProcessedCount(), tracker.getTotalUsers());
    }
    
    /**
     * 작업 완료 처리
     */
    private void completeJob(UUID jobId, JobProgressTracker tracker) {
        LocalDateTime completedAt = LocalDateTime.now();
        BulkMessageJobStatus currentStatus = jobStatusMap.get(jobId);
        
        if (currentStatus == null) return;
        
        Duration duration = Duration.between(currentStatus.startedAt(), completedAt);
        
        // 작업 상태 결정
        BulkMessageResponse.JobStatus finalStatus;
        if (tracker.getFailureCount() == 0) {
            finalStatus = BulkMessageResponse.JobStatus.COMPLETED;
        } else if (tracker.getSuccessCount() > 0) {
            finalStatus = BulkMessageResponse.JobStatus.PARTIALLY_FAILED;
        } else {
            finalStatus = BulkMessageResponse.JobStatus.FAILED;
        }
        
        BulkMessageJobStatus completedStatus = new BulkMessageJobStatus(
            jobId,
            finalStatus,
            tracker.getTotalUsers(),
            tracker.getProcessedCount(),
            tracker.getSuccessCount(),
            tracker.getFailureCount(),
            currentStatus.startedAt(),
            completedAt,
            duration,
            100.0
        );
        
        jobStatusMap.put(jobId, completedStatus);
        
        log.info("메시지 발송 작업 완료 - jobId: {}, status: {}, total: {}, success: {}, failure: {}, duration: {}ms", 
                jobId, finalStatus, tracker.getTotalUsers(), tracker.getSuccessCount(), 
                tracker.getFailureCount(), duration.toMillis());
        
        // 구조화된 작업 완료 로그 (발송 통계 포함)
        MessageSendTracker.SendStatistics sendStats = messageSendTracker.getStatistics();
        log.info("작업 완료 시점의 전체 발송 통계 - 전체시도: {}, 큐상태: {}/{}", 
            sendStats.totalAttempts(), sendStats.currentQueueSize(), sendStats.maxQueueSize());
            
        structuredLogger.logJobCompletion(jobId, finalStatus.toString(), 
            tracker.getTotalUsers(), tracker.getSuccessCount(), tracker.getFailureCount(), duration.toMillis());
        
        // 추적기 정리
        jobProgressMap.remove(jobId);
    }
    
    /**
     * 작업 상태 업데이트
     */
    private void updateJobStatus(UUID jobId, BulkMessageResponse.JobStatus status) {
        BulkMessageJobStatus currentStatus = jobStatusMap.get(jobId);
        if (currentStatus == null) return;
        
        BulkMessageJobStatus updatedStatus = new BulkMessageJobStatus(
            currentStatus.jobId(),
            status,
            currentStatus.totalUsers(),
            currentStatus.processedUsers(),
            currentStatus.successCount(),
            currentStatus.failureCount(),
            currentStatus.startedAt(),
            currentStatus.completedAt(),
            currentStatus.duration(),
            currentStatus.progressPercentage()
        );
        
        jobStatusMap.put(jobId, updatedStatus);
    }
    
    /**
     * 빈 응답 생성 (사용자가 없는 경우)
     */
    private BulkMessageResponse createEmptyResponse(UUID jobId) {
        BulkMessageJobStatus emptyStatus = new BulkMessageJobStatus(
            jobId,
            BulkMessageResponse.JobStatus.COMPLETED,
            0, 0, 0, 0,
            LocalDateTime.now(),
            LocalDateTime.now(),
            Duration.ZERO,
            100.0
        );
        
        jobStatusMap.put(jobId, emptyStatus);
        
        return new BulkMessageResponse(
            jobId, 0, Duration.ZERO,
            BulkMessageResponse.JobStatus.COMPLETED,
            LocalDateTime.now()
        );
    }
    
    /**
     * 작업 진행 상태 추적기
     */
    private static class JobProgressTracker {
        private final UUID jobId;
        private final int totalUsers;
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger batchNumber = new AtomicInteger(0);
        
        public JobProgressTracker(UUID jobId, int totalUsers) {
            this.jobId = jobId;
            this.totalUsers = totalUsers;
        }
        
        public void incrementProcessed() { processedCount.incrementAndGet(); }
        public void incrementSuccess() { successCount.incrementAndGet(); }
        public void incrementFailure() { failureCount.incrementAndGet(); }
        public int getBatchNumber() { return batchNumber.incrementAndGet(); }
        
        public int getTotalUsers() { return totalUsers; }
        public int getProcessedCount() { return processedCount.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
    }
    
    /**
     * 전화번호를 마스킹합니다.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        
        if (phoneNumber.contains("-") && phoneNumber.length() == 13) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        return "***-****-****";
    }
}