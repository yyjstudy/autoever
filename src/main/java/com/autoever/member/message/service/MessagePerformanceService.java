package com.autoever.member.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 메시지 발송 성능 모니터링 서비스
 */
@Slf4j
@Service
public class MessagePerformanceService {
    
    // 성능 메트릭 저장소
    private final ConcurrentHashMap<String, PerformanceMetrics> metricsMap = new ConcurrentHashMap<>();
    
    // 전체 시스템 메트릭
    private final SystemMetrics systemMetrics = new SystemMetrics();
    
    /**
     * 작업 성능 추적 시작
     */
    public PerformanceTracker startTracking(String jobId, int totalUsers) {
        PerformanceMetrics metrics = new PerformanceMetrics(jobId, totalUsers);
        metricsMap.put(jobId, metrics);
        
        log.info("성능 추적 시작 - jobId: {}, totalUsers: {}", jobId, totalUsers);
        
        return new PerformanceTracker(jobId, metrics, systemMetrics);
    }
    
    /**
     * 작업 성능 추적 종료
     */
    public PerformanceMetrics stopTracking(String jobId) {
        PerformanceMetrics metrics = metricsMap.remove(jobId);
        if (metrics != null) {
            metrics.markCompleted();
            logPerformanceSummary(metrics);
        }
        return metrics;
    }
    
    /**
     * 현재 진행 중인 작업의 성능 메트릭 조회
     */
    public PerformanceMetrics getMetrics(String jobId) {
        return metricsMap.get(jobId);
    }
    
    /**
     * 시스템 전체 성능 메트릭 조회
     */
    public SystemMetrics getSystemMetrics() {
        return systemMetrics;
    }
    
    /**
     * 성능 요약 로그 출력
     */
    private void logPerformanceSummary(PerformanceMetrics metrics) {
        double throughputPerSecond = metrics.getThroughputPerSecond();
        double avgResponseTime = metrics.getAverageResponseTime();
        
        log.info("성능 추적 완료 - jobId: {}, " +
                "처리량: {:.1f}msg/s, " +
                "평균 응답시간: {:.1f}ms, " +
                "성공률: {:.1f}%, " +
                "총 처리시간: {}ms",
                metrics.getJobId(),
                throughputPerSecond,
                avgResponseTime,
                metrics.getSuccessRate(),
                metrics.getTotalDurationMs());
    }
    
    /**
     * 성능 추적기
     */
    public static class PerformanceTracker {
        private final String jobId;
        private final PerformanceMetrics jobMetrics;
        private final SystemMetrics systemMetrics;
        
        public PerformanceTracker(String jobId, PerformanceMetrics jobMetrics, SystemMetrics systemMetrics) {
            this.jobId = jobId;
            this.jobMetrics = jobMetrics;
            this.systemMetrics = systemMetrics;
        }
        
        /**
         * 메시지 발송 성공 기록
         */
        public void recordSuccess(long responseTimeMs) {
            jobMetrics.recordSuccess(responseTimeMs);
            systemMetrics.recordSuccess(responseTimeMs);
        }
        
        /**
         * 메시지 발송 실패 기록
         */
        public void recordFailure(long responseTimeMs) {
            jobMetrics.recordFailure(responseTimeMs);
            systemMetrics.recordFailure(responseTimeMs);
        }
        
        /**
         * 배치 처리 완료 기록
         */
        public void recordBatchProcessed(int batchSize, long durationMs) {
            jobMetrics.recordBatchProcessed(batchSize, durationMs);
            systemMetrics.recordBatchProcessed(batchSize, durationMs);
        }
        
        public PerformanceMetrics getMetrics() {
            return jobMetrics;
        }
        
        public String getJobId() {
            return jobId;
        }
    }
    
    /**
     * 개별 작업 성능 메트릭
     */
    public static class PerformanceMetrics {
        private final String jobId;
        private final int totalUsers;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger batchCount = new AtomicInteger(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        
        public PerformanceMetrics(String jobId, int totalUsers) {
            this.jobId = jobId;
            this.totalUsers = totalUsers;
            this.startTime = LocalDateTime.now();
        }
        
        public void recordSuccess(long responseTimeMs) {
            successCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
        }
        
        public void recordFailure(long responseTimeMs) {
            failureCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
        }
        
        public void recordBatchProcessed(int batchSize, long durationMs) {
            batchCount.incrementAndGet();
        }
        
        public void markCompleted() {
            this.endTime = LocalDateTime.now();
        }
        
        // Getters
        public String getJobId() { return jobId; }
        public int getTotalUsers() { return totalUsers; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
        public int getBatchCount() { return batchCount.get(); }
        public int getTotalProcessed() { return successCount.get() + failureCount.get(); }
        
        public double getSuccessRate() {
            int total = getTotalProcessed();
            return total > 0 ? (double) successCount.get() / total * 100 : 0.0;
        }
        
        public double getThroughputPerSecond() {
            long durationMs = getTotalDurationMs();
            return durationMs > 0 ? (double) getTotalProcessed() / durationMs * 1000 : 0.0;
        }
        
        public double getAverageResponseTime() {
            int total = getTotalProcessed();
            return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
        }
        
        public long getTotalDurationMs() {
            if (endTime != null) {
                return Duration.between(startTime, endTime).toMillis();
            }
            return Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
    }
    
    /**
     * 시스템 전체 성능 메트릭
     */
    public static class SystemMetrics {
        private final LongAdder totalSuccessCount = new LongAdder();
        private final LongAdder totalFailureCount = new LongAdder();
        private final LongAdder totalBatchCount = new LongAdder();
        private final LongAdder totalResponseTime = new LongAdder();
        private final LocalDateTime systemStartTime = LocalDateTime.now();
        
        public void recordSuccess(long responseTimeMs) {
            totalSuccessCount.increment();
            totalResponseTime.add(responseTimeMs);
        }
        
        public void recordFailure(long responseTimeMs) {
            totalFailureCount.increment();
            totalResponseTime.add(responseTimeMs);
        }
        
        public void recordBatchProcessed(int batchSize, long durationMs) {
            totalBatchCount.increment();
        }
        
        // Getters
        public long getTotalSuccessCount() { return totalSuccessCount.sum(); }
        public long getTotalFailureCount() { return totalFailureCount.sum(); }
        public long getTotalBatchCount() { return totalBatchCount.sum(); }
        public long getTotalProcessed() { return getTotalSuccessCount() + getTotalFailureCount(); }
        
        public double getOverallSuccessRate() {
            long total = getTotalProcessed();
            return total > 0 ? (double) getTotalSuccessCount() / total * 100 : 0.0;
        }
        
        public double getOverallThroughputPerSecond() {
            long systemUptimeMs = Duration.between(systemStartTime, LocalDateTime.now()).toMillis();
            return systemUptimeMs > 0 ? (double) getTotalProcessed() / systemUptimeMs * 1000 : 0.0;
        }
        
        public double getOverallAverageResponseTime() {
            long total = getTotalProcessed();
            return total > 0 ? (double) totalResponseTime.sum() / total : 0.0;
        }
        
        public LocalDateTime getSystemStartTime() { return systemStartTime; }
    }
}