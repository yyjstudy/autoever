package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.AgeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 대량 데이터 배치 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessingService {
    
    private final UserQueryService userQueryService;
    private final AgeCalculationService ageCalculationService;
    
    @Qualifier("messageTaskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;
    
    // 메시지 발송을 위한 서브 배치 크기
    private static final int MESSAGE_SUB_BATCH_SIZE = 100;
    
    /**
     * 연령대별 사용자 대상 배치 작업 실행
     * 
     * @param ageGroup 연령대
     * @param jobId 작업 ID
     * @param batchProcessor 배치 처리 함수 (사용자 리스트, 진행률)
     * @return 전체 처리 결과
     */
    public BatchProcessingResult processUsersByAgeGroup(
            AgeGroup ageGroup, 
            UUID jobId,
            BiConsumer<List<User>, BatchProgress> batchProcessor) {
        
        AgeRange ageRange = ageCalculationService.calculateAgeRange(ageGroup);
        long totalUsers = userQueryService.countUsersByAgeRange(ageRange);
        
        log.info("배치 작업 시작 - jobId: {}, 연령대: {}, 예상 사용자: {}", 
                jobId, ageGroup, totalUsers);
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 페이지 단위로 사용자 처리
        userQueryService.processUsersByAgeRangeInBatches(ageRange, users -> {
            // 서브 배치로 분할하여 처리
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            userQueryService.processInSubBatches(users, MESSAGE_SUB_BATCH_SIZE, subBatch -> {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        BatchProgress progress = new BatchProgress(
                            jobId,
                            totalUsers,
                            processedCount.get(),
                            successCount.get(),
                            failureCount.get()
                        );
                        
                        batchProcessor.accept(subBatch, progress);
                        
                        // 성공으로 가정 (실제로는 batchProcessor에서 결과 반환)
                        successCount.addAndGet(subBatch.size());
                        
                    } catch (Exception e) {
                        log.error("서브 배치 처리 실패 - jobId: {}, 크기: {}", 
                                jobId, subBatch.size(), e);
                        failureCount.addAndGet(subBatch.size());
                    } finally {
                        int processed = processedCount.addAndGet(subBatch.size());
                        if (processed % 1000 == 0) {
                            double progressPercentage = (double) processed / totalUsers * 100;
                            log.info("진행률 - jobId: {}, 처리: {}/{} ({}%)", 
                                    jobId, processed, totalUsers, 
                                    String.format("%.1f", progressPercentage));
                        }
                    }
                }, taskExecutor);
                
                futures.add(future);
            });
            
            // 현재 페이지의 모든 서브 배치가 완료될 때까지 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        });
        
        long duration = System.currentTimeMillis() - startTime;
        
        BatchProcessingResult result = new BatchProcessingResult(
            jobId,
            totalUsers,
            processedCount.get(),
            successCount.get(),
            failureCount.get(),
            duration
        );
        
        log.info("배치 작업 완료 - {}", result);
        
        return result;
    }
    
    /**
     * 콜백 기반 배치 처리 (BulkMessageService용)
     * 
     * @param ageGroup 연령대
     * @param batchCallback 배치별 콜백 (사용자 리스트 처리)
     * @param progressCallback 진행률 콜백
     */
    public void processBatchWithCallback(
            AgeGroup ageGroup,
            Consumer<List<User>> batchCallback,
            Consumer<BatchProgress> progressCallback) {
        
        UUID jobId = UUID.randomUUID(); // 임시 ID
        AgeRange ageRange = ageCalculationService.calculateAgeRange(ageGroup);
        long totalUsers = userQueryService.countUsersByAgeRange(ageRange);
        
        log.info("콜백 배치 처리 시작 - 연령대: {}, 총 사용자: {}", ageGroup, totalUsers);
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        // 페이지 단위로 사용자 처리
        userQueryService.processUsersByAgeRangeInBatches(ageRange, users -> {
            // 배치 콜백 실행
            batchCallback.accept(users);
            
            // 진행률 업데이트
            int processed = processedCount.addAndGet(users.size());
            BatchProgress progress = new BatchProgress(
                jobId, totalUsers, processed, processed, 0 // success는 외부에서 관리
            );
            
            progressCallback.accept(progress);
            
            if (processed % 1000 == 0 || processed == totalUsers) {
                log.debug("배치 진행률 - 처리: {}/{} ({}%)", 
                         processed, totalUsers, progress.getProgressPercentage());
            }
        });
        
        log.info("콜백 배치 처리 완료 - 연령대: {}, 처리된 사용자: {}", ageGroup, processedCount.get());
    }
    
    /**
     * 스레드 풀 상태 조회
     */
    public ThreadPoolStatus getThreadPoolStatus() {
        return new ThreadPoolStatus(
            taskExecutor.getCorePoolSize(),
            taskExecutor.getMaxPoolSize(),
            taskExecutor.getActiveCount(),
            taskExecutor.getQueueSize(),
            taskExecutor.getQueueCapacity(),
            taskExecutor.getPoolSize(),
            taskExecutor.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }
    
    /**
     * 배치 처리 진행 상황
     */
    public record BatchProgress(
        UUID jobId,
        long totalUsers,
        int processedUsers,
        int successCount,
        int failureCount
    ) {
        public double getProgressPercentage() {
            if (totalUsers == 0) return 100.0;
            return Math.round((double) processedUsers / totalUsers * 100 * 100) / 100.0;
        }
        
        public double getSuccessRate() {
            if (processedUsers == 0) return 0.0;
            return Math.round((double) successCount / processedUsers * 100 * 100) / 100.0;
        }
    }
    
    /**
     * 배치 처리 결과
     */
    public record BatchProcessingResult(
        UUID jobId,
        long totalUsers,
        int processedUsers,
        int successCount,
        int failureCount,
        long durationMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "jobId: %s, 총: %d, 처리: %d, 성공: %d, 실패: %d, 소요시간: %dms",
                jobId, totalUsers, processedUsers, successCount, failureCount, durationMs
            );
        }
    }
    
    /**
     * 스레드 풀 상태
     */
    public record ThreadPoolStatus(
        int corePoolSize,
        int maxPoolSize,
        int activeCount,
        int queueSize,
        int queueCapacity,
        int poolSize,
        long completedTaskCount
    ) {
        public double getUtilization() {
            if (maxPoolSize == 0) return 0.0;
            return Math.round((double) activeCount / maxPoolSize * 100 * 100) / 100.0;
        }
        
        public double getQueueUtilization() {
            if (queueCapacity == 0) return 0.0;
            return Math.round((double) queueSize / queueCapacity * 100 * 100) / 100.0;
        }
    }
}