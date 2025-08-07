package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
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
        
        // 비동기 발송 시작
        processMessageSendingAsync(jobId, ageGroup, request.message(), totalUsers);
        
        return response;
    }
    
    
    /**
     * 비동기 메시지 발송 처리
     */
    @Async("messageTaskExecutor")
    public CompletableFuture<Void> processMessageSendingAsync(UUID jobId, AgeGroup ageGroup, String message, int totalUsers) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("비동기 메시지 발송 시작 - jobId: {}, totalUsers: {}", jobId, totalUsers);
                
                // 진행 상태 추적기 초기화
                JobProgressTracker tracker = new JobProgressTracker(jobId, totalUsers);
                
                // 배치 처리로 사용자 조회 및 메시지 발송
                batchProcessingService.processBatchWithCallback(
                    ageGroup,
                    users -> sendMessagesToUsers(jobId, users, message, tracker),
                    progress -> logJobProgress(jobId, progress, tracker)
                );
                
                // 최종 완료 처리
                completeJob(jobId, tracker);
                
            } catch (Exception e) {
                log.error("메시지 발송 중 오류 발생 - jobId: {}", jobId, e);
            }
        });
    }
    
    /**
     * 사용자 리스트에게 메시지 발송
     */
    private void sendMessagesToUsers(UUID jobId, List<User> users, String message, JobProgressTracker tracker) {
        long batchStartTime = System.currentTimeMillis();
        log.debug("배치 메시지 발송 - jobId: {}, userCount: {}", jobId, users.size());
        
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
     * 작업 진행 상황 로그
     */
    private void logJobProgress(UUID jobId, BatchProcessingService.BatchProgress batchProgress, JobProgressTracker tracker) {
        double progressPercentage = (double) tracker.getProcessedCount() / tracker.getTotalUsers() * 100.0;
        
        log.debug("작업 진행 상황 - jobId: {}, progress: {:.1f}%, processed: {}/{}", 
                 jobId, progressPercentage, tracker.getProcessedCount(), tracker.getTotalUsers());
    }
    
    /**
     * 작업 완료 처리
     */
    private void completeJob(UUID jobId, JobProgressTracker tracker) {
        // 작업 상태 결정
        String finalStatus;
        if (tracker.getFailureCount() == 0) {
            finalStatus = "COMPLETED";
        } else if (tracker.getSuccessCount() > 0) {
            finalStatus = "PARTIALLY_FAILED";
        } else {
            finalStatus = "FAILED";
        }
        
        log.info("메시지 발송 작업 완료 - jobId: {}, status: {}, total: {}, success: {}, failure: {}", 
                jobId, finalStatus, tracker.getTotalUsers(), tracker.getSuccessCount(), 
                tracker.getFailureCount());
        
        // 구조화된 작업 완료 로그 (발송 통계 포함)
        MessageSendTracker.SendStatistics sendStats = messageSendTracker.getStatistics();
        log.info("작업 완료 시점의 전체 발송 통계 - 전체시도: {}, 큐상태: {}/{}", 
            sendStats.totalAttempts(), sendStats.currentQueueSize(), sendStats.maxQueueSize());
            
        structuredLogger.logJobCompletion(jobId, finalStatus, 
            tracker.getTotalUsers(), tracker.getSuccessCount(), tracker.getFailureCount(), 0L);
    }
    
    
    /**
     * 빈 응답 생성 (사용자가 없는 경우)
     */
    private BulkMessageResponse createEmptyResponse(UUID jobId) {
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