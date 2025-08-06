package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.service.ExternalMessageService;
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
        
        // 실제 사용자 수 조회
        int totalUsers = userQueryService.countUsersByAgeGroup(ageGroup);
        
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
     * 작업 상태 조회
     */
    public BulkMessageJobStatus getJobStatus(UUID jobId) {
        BulkMessageJobStatus status = jobStatusMap.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("존재하지 않는 작업 ID입니다: " + jobId);
        }
        return status;
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
        log.debug("배치 메시지 발송 - jobId: {}, userCount: {}", jobId, users.size());
        
        updateJobStatus(jobId, BulkMessageResponse.JobStatus.SENDING_MESSAGES);
        
        for (User user : users) {
            try {
                // 외부 메시지 서비스를 통한 실제 발송
                externalMessageService.sendMessage(user.getPhoneNumber(), message);
                tracker.incrementSuccess();
                
                log.trace("메시지 발송 성공 - userId: {}, phone: {}", 
                         user.getId(), user.getPhoneNumber());
                
            } catch (Exception e) {
                tracker.incrementFailure();
                log.warn("메시지 발송 실패 - userId: {}, phone: {}, error: {}", 
                        user.getId(), user.getPhoneNumber(), e.getMessage());
            }
            
            tracker.incrementProcessed();
        }
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
        
        public JobProgressTracker(UUID jobId, int totalUsers) {
            this.jobId = jobId;
            this.totalUsers = totalUsers;
        }
        
        public void incrementProcessed() { processedCount.incrementAndGet(); }
        public void incrementSuccess() { successCount.incrementAndGet(); }
        public void incrementFailure() { failureCount.incrementAndGet(); }
        
        public int getTotalUsers() { return totalUsers; }
        public int getProcessedCount() { return processedCount.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
    }
}