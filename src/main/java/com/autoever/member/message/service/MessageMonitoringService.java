package com.autoever.member.message.service;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.retry.RetryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 메시지 발송 모니터링 서비스
 * 성능 메트릭, 성공/실패율, 응답 시간 등을 추적합니다.
 */
@Service
public class MessageMonitoringService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageMonitoringService.class);
    
    // 전체 통계
    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong totalSuccesses = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);
    
    // API 타입별 통계
    private final Map<ApiType, ApiTypeMetrics> apiMetrics = new ConcurrentHashMap<>();
    
    // 최근 발송 기록 (최대 1000개)
    private final List<MessageAttemptRecord> recentAttempts = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_RECENT_RECORDS = 1000;
    
    // 서비스 시작 시간
    private final LocalDateTime serviceStartTime = LocalDateTime.now();
    
    public MessageMonitoringService() {
        // API 타입별 메트릭 초기화
        for (ApiType apiType : ApiType.values()) {
            apiMetrics.put(apiType, new ApiTypeMetrics());
        }
        
        log.info("MessageMonitoringService 초기화 완료 - 시작 시간: {}", serviceStartTime);
    }
    
    /**
     * 메시지 발송 시도 기록
     */
    public void recordMessageAttempt(RetryContext.RetrySummary summary) {
        totalAttempts.incrementAndGet();
        
        if (summary.isFinalSuccess()) {
            totalSuccesses.incrementAndGet();
        } else {
            totalFailures.incrementAndGet();
        }
        
        // API 타입별 통계 업데이트
        for (RetryContext.RetryAttempt attempt : summary.getAttempts()) {
            ApiTypeMetrics metrics = apiMetrics.get(attempt.getApiType());
            if (metrics != null) {
                metrics.recordAttempt(attempt.isSuccess(), attempt.getDuration().toMillis());
            }
        }
        
        // 최근 기록 저장
        MessageAttemptRecord record = new MessageAttemptRecord(summary);
        synchronized (recentAttempts) {
            recentAttempts.add(record);
            
            // 최대 개수 초과 시 오래된 기록 제거
            while (recentAttempts.size() > MAX_RECENT_RECORDS) {
                recentAttempts.remove(0);
            }
        }
        
        // 주기적으로 통계 로그 출력 (100번마다)
        long currentTotal = totalAttempts.get();
        if (currentTotal % 100 == 0) {
            logCurrentStatistics();
        }
    }
    
    /**
     * 현재 통계 정보 반환
     */
    public MonitoringStatistics getCurrentStatistics() {
        Map<ApiType, ApiTypeStatistics> apiStats = new ConcurrentHashMap<>();
        
        for (Map.Entry<ApiType, ApiTypeMetrics> entry : apiMetrics.entrySet()) {
            ApiTypeMetrics metrics = entry.getValue();
            apiStats.put(entry.getKey(), new ApiTypeStatistics(
                metrics.totalAttempts.get(),
                metrics.successfulAttempts.get(),
                metrics.failedAttempts.get(),
                metrics.getTotalResponseTime(),
                metrics.getAverageResponseTime()
            ));
        }
        
        return new MonitoringStatistics(
            serviceStartTime,
            LocalDateTime.now(),
            totalAttempts.get(),
            totalSuccesses.get(),
            totalFailures.get(),
            apiStats,
            new ArrayList<>(recentAttempts)
        );
    }
    
    /**
     * 통계 초기화
     */
    public void resetStatistics() {
        log.info("메시지 모니터링 통계 초기화");
        
        totalAttempts.set(0);
        totalSuccesses.set(0);
        totalFailures.set(0);
        
        for (ApiTypeMetrics metrics : apiMetrics.values()) {
            metrics.reset();
        }
        
        synchronized (recentAttempts) {
            recentAttempts.clear();
        }
    }
    
    /**
     * 현재 통계를 로그로 출력
     */
    public void logCurrentStatistics() {
        MonitoringStatistics stats = getCurrentStatistics();
        
        log.info("=== 메시지 발송 통계 ===");
        log.info("전체 시도: {}, 성공: {}, 실패: {}, 성공률: {:.2f}%", 
            stats.getTotalAttempts(),
            stats.getTotalSuccesses(), 
            stats.getTotalFailures(),
            stats.getSuccessRate()
        );
        
        for (Map.Entry<ApiType, ApiTypeStatistics> entry : stats.getApiStatistics().entrySet()) {
            ApiTypeStatistics apiStats = entry.getValue();
            log.info("{} - 시도: {}, 성공: {}, 실패: {}, 성공률: {:.2f}%, 평균응답시간: {}ms",
                entry.getKey().getDisplayName(),
                apiStats.getTotalAttempts(),
                apiStats.getSuccessfulAttempts(),
                apiStats.getFailedAttempts(),
                apiStats.getSuccessRate(),
                apiStats.getAverageResponseTime()
            );
        }
    }
    
    /**
     * API 타입별 메트릭
     */
    private static class ApiTypeMetrics {
        private final AtomicLong totalAttempts = new AtomicLong(0);
        private final AtomicLong successfulAttempts = new AtomicLong(0);
        private final AtomicLong failedAttempts = new AtomicLong(0);
        private final LongAdder totalResponseTime = new LongAdder();
        
        void recordAttempt(boolean success, long responseTimeMs) {
            totalAttempts.incrementAndGet();
            totalResponseTime.add(responseTimeMs);
            
            if (success) {
                successfulAttempts.incrementAndGet();
            } else {
                failedAttempts.incrementAndGet();
            }
        }
        
        void reset() {
            totalAttempts.set(0);
            successfulAttempts.set(0);
            failedAttempts.set(0);
            totalResponseTime.reset();
        }
        
        long getTotalResponseTime() {
            return totalResponseTime.sum();
        }
        
        long getAverageResponseTime() {
            long total = totalAttempts.get();
            return total > 0 ? getTotalResponseTime() / total : 0;
        }
    }
    
    /**
     * 개별 메시지 발송 시도 기록
     */
    public static class MessageAttemptRecord {
        private final LocalDateTime timestamp;
        private final String recipientPhone;
        private final int totalAttempts;
        private final boolean finalSuccess;
        private final long totalDurationMs;
        private final String failureReason;
        private final List<String> apiTypesUsed;
        
        public MessageAttemptRecord(RetryContext.RetrySummary summary) {
            this.timestamp = summary.getEndTime();
            this.recipientPhone = maskPhoneNumber(summary.getOriginalRequest().recipient());
            this.totalAttempts = summary.getTotalAttempts();
            this.finalSuccess = summary.isFinalSuccess();
            this.totalDurationMs = summary.getTotalDuration().toMillis();
            this.failureReason = summary.getLastException() != null ? summary.getLastException().getMessage() : null;
            
            this.apiTypesUsed = summary.getAttempts().stream()
                .map(attempt -> attempt.getApiType().getDisplayName())
                .distinct()
                .toList();
        }
        
        private String maskPhoneNumber(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.length() < 4) {
                return "****";
            }
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getRecipientPhone() { return recipientPhone; }
        public int getTotalAttempts() { return totalAttempts; }
        public boolean isFinalSuccess() { return finalSuccess; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public String getFailureReason() { return failureReason; }
        public List<String> getApiTypesUsed() { return apiTypesUsed; }
    }
    
    /**
     * API 타입별 통계
     */
    public static class ApiTypeStatistics {
        private final long totalAttempts;
        private final long successfulAttempts;
        private final long failedAttempts;
        private final long totalResponseTime;
        private final long averageResponseTime;
        
        public ApiTypeStatistics(long totalAttempts, long successfulAttempts, long failedAttempts, 
                               long totalResponseTime, long averageResponseTime) {
            this.totalAttempts = totalAttempts;
            this.successfulAttempts = successfulAttempts;
            this.failedAttempts = failedAttempts;
            this.totalResponseTime = totalResponseTime;
            this.averageResponseTime = averageResponseTime;
        }
        
        public double getSuccessRate() {
            return totalAttempts > 0 ? (double) successfulAttempts / totalAttempts * 100.0 : 0.0;
        }
        
        // Getters
        public long getTotalAttempts() { return totalAttempts; }
        public long getSuccessfulAttempts() { return successfulAttempts; }
        public long getFailedAttempts() { return failedAttempts; }
        public long getTotalResponseTime() { return totalResponseTime; }
        public long getAverageResponseTime() { return averageResponseTime; }
    }
    
    /**
     * 전체 모니터링 통계
     */
    public static class MonitoringStatistics {
        private final LocalDateTime serviceStartTime;
        private final LocalDateTime currentTime;
        private final long totalAttempts;
        private final long totalSuccesses;
        private final long totalFailures;
        private final Map<ApiType, ApiTypeStatistics> apiStatistics;
        private final List<MessageAttemptRecord> recentAttempts;
        
        public MonitoringStatistics(LocalDateTime serviceStartTime, LocalDateTime currentTime,
                                  long totalAttempts, long totalSuccesses, long totalFailures,
                                  Map<ApiType, ApiTypeStatistics> apiStatistics,
                                  List<MessageAttemptRecord> recentAttempts) {
            this.serviceStartTime = serviceStartTime;
            this.currentTime = currentTime;
            this.totalAttempts = totalAttempts;
            this.totalSuccesses = totalSuccesses;
            this.totalFailures = totalFailures;
            this.apiStatistics = apiStatistics;
            this.recentAttempts = recentAttempts;
        }
        
        public long getUptimeHours() {
            return ChronoUnit.HOURS.between(serviceStartTime, currentTime);
        }
        
        public double getSuccessRate() {
            return totalAttempts > 0 ? (double) totalSuccesses / totalAttempts * 100.0 : 0.0;
        }
        
        public double getFailureRate() {
            return totalAttempts > 0 ? (double) totalFailures / totalAttempts * 100.0 : 0.0;
        }
        
        // Getters
        public LocalDateTime getServiceStartTime() { return serviceStartTime; }
        public LocalDateTime getCurrentTime() { return currentTime; }
        public long getTotalAttempts() { return totalAttempts; }
        public long getTotalSuccesses() { return totalSuccesses; }
        public long getTotalFailures() { return totalFailures; }
        public Map<ApiType, ApiTypeStatistics> getApiStatistics() { return apiStatistics; }
        public List<MessageAttemptRecord> getRecentAttempts() { return recentAttempts; }
    }
}