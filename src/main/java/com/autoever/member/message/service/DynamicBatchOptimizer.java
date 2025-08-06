package com.autoever.member.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 동적 배치 크기 최적화 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicBatchOptimizer {
    
    private final BatchProcessingService batchProcessingService;
    
    // 배치 크기 설정
    private static final int MIN_BATCH_SIZE = 50;
    private static final int MAX_BATCH_SIZE = 2000;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    
    // 성능 임계값
    private static final double HIGH_CPU_THRESHOLD = 80.0;
    private static final double LOW_CPU_THRESHOLD = 30.0;
    private static final int HIGH_QUEUE_SIZE_THRESHOLD = 800;
    private static final int LOW_QUEUE_SIZE_THRESHOLD = 200;
    
    private final AtomicInteger currentBatchSize = new AtomicInteger(DEFAULT_BATCH_SIZE);
    
    /**
     * 현재 시스템 상태에 기반한 최적 배치 크기 계산
     * 
     * @return 최적화된 배치 크기
     */
    public int getOptimalBatchSize() {
        BatchProcessingService.ThreadPoolStatus threadPoolStatus = batchProcessingService.getThreadPoolStatus();
        
        double cpuUtilization = threadPoolStatus.getUtilization();
        double queueUtilization = threadPoolStatus.getQueueUtilization();
        int queueSize = threadPoolStatus.queueSize();
        
        int newBatchSize = calculateOptimalSize(cpuUtilization, queueUtilization, queueSize);
        int oldBatchSize = currentBatchSize.getAndSet(newBatchSize);
        
        if (newBatchSize != oldBatchSize) {
            log.info("배치 크기 조정 - {} -> {} (CPU: {:.1f}%, Queue: {}/{})",
                    oldBatchSize, newBatchSize, cpuUtilization, queueSize, threadPoolStatus.queueCapacity());
        }
        
        return newBatchSize;
    }
    
    /**
     * 현재 배치 크기 조회
     */
    public int getCurrentBatchSize() {
        return currentBatchSize.get();
    }
    
    /**
     * 배치 크기 강제 설정 (테스트 또는 수동 조정용)
     */
    public void setBatchSize(int batchSize) {
        int adjustedSize = Math.max(MIN_BATCH_SIZE, Math.min(MAX_BATCH_SIZE, batchSize));
        currentBatchSize.set(adjustedSize);
        log.info("배치 크기 수동 설정: {}", adjustedSize);
    }
    
    /**
     * 최적 배치 크기 계산 로직
     */
    private int calculateOptimalSize(double cpuUtilization, double queueUtilization, int queueSize) {
        int currentSize = currentBatchSize.get();
        
        // CPU 사용률이 높으면 배치 크기 감소
        if (cpuUtilization > HIGH_CPU_THRESHOLD) {
            return Math.max(MIN_BATCH_SIZE, (int) (currentSize * 0.8));
        }
        
        // 큐가 꽉 차면 배치 크기 증가하여 처리량 향상
        if (queueSize > HIGH_QUEUE_SIZE_THRESHOLD) {
            return Math.min(MAX_BATCH_SIZE, (int) (currentSize * 1.5));
        }
        
        // CPU 사용률이 낮고 큐가 비어있으면 배치 크기 증가
        if (cpuUtilization < LOW_CPU_THRESHOLD && queueSize < LOW_QUEUE_SIZE_THRESHOLD) {
            return Math.min(MAX_BATCH_SIZE, (int) (currentSize * 1.2));
        }
        
        // 적절한 상태면 현재 크기 유지
        return currentSize;
    }
    
    /**
     * 시스템 부하에 따른 배치 크기 추천
     */
    public BatchSizeRecommendation getRecommendation() {
        BatchProcessingService.ThreadPoolStatus status = batchProcessingService.getThreadPoolStatus();
        int optimalSize = getOptimalBatchSize();
        
        SystemLoadLevel loadLevel = determineLoadLevel(status);
        String reason = generateReasonMessage(status, loadLevel);
        
        return new BatchSizeRecommendation(optimalSize, loadLevel, reason, status);
    }
    
    /**
     * 시스템 부하 수준 결정
     */
    private SystemLoadLevel determineLoadLevel(BatchProcessingService.ThreadPoolStatus status) {
        double cpuUtilization = status.getUtilization();
        double queueUtilization = status.getQueueUtilization();
        
        if (cpuUtilization > HIGH_CPU_THRESHOLD || queueUtilization > 90) {
            return SystemLoadLevel.HIGH;
        } else if (cpuUtilization > 50 || queueUtilization > 60) {
            return SystemLoadLevel.MEDIUM;
        } else {
            return SystemLoadLevel.LOW;
        }
    }
    
    /**
     * 추천 사유 메시지 생성
     */
    private String generateReasonMessage(BatchProcessingService.ThreadPoolStatus status, SystemLoadLevel loadLevel) {
        return switch (loadLevel) {
            case HIGH -> String.format("높은 시스템 부하 (CPU: %.1f%%, Queue: %d/%d)", 
                    status.getUtilization(), status.queueSize(), status.queueCapacity());
            case MEDIUM -> String.format("중간 시스템 부하 (CPU: %.1f%%, Queue: %d/%d)", 
                    status.getUtilization(), status.queueSize(), status.queueCapacity());
            case LOW -> String.format("낮은 시스템 부하 (CPU: %.1f%%, Queue: %d/%d)", 
                    status.getUtilization(), status.queueSize(), status.queueCapacity());
        };
    }
    
    /**
     * 시스템 부하 수준
     */
    public enum SystemLoadLevel {
        LOW("낮음"),
        MEDIUM("중간"),
        HIGH("높음");
        
        private final String description;
        
        SystemLoadLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 배치 크기 추천 결과
     */
    public record BatchSizeRecommendation(
        int recommendedSize,
        SystemLoadLevel loadLevel,
        String reason,
        BatchProcessingService.ThreadPoolStatus threadPoolStatus
    ) {
        
        public boolean isOptimizationNeeded(int currentSize) {
            return currentSize != recommendedSize;
        }
        
        @Override
        public String toString() {
            return String.format("BatchSizeRecommendation{size=%d, load=%s, reason='%s'}", 
                    recommendedSize, loadLevel.getDescription(), reason);
        }
    }
}