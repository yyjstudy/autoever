package com.autoever.member.message.ratelimit;

import com.autoever.member.message.ApiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * API별 Rate Limiting을 처리하는 클래스
 * 카카오톡: 100회/분, SMS: 500회/분의 제한 정책 적용
 */
@Component
public class ApiRateLimiter {
    
    private static final Logger log = LoggerFactory.getLogger(ApiRateLimiter.class);
    
    // API별 제한 정책
    private static final int KAKAOTALK_LIMIT_PER_MINUTE = 100;
    private static final int SMS_LIMIT_PER_MINUTE = 500;
    
    // API별 현재 카운트 및 윈도우 시작 시간
    private final ConcurrentHashMap<ApiType, AtomicInteger> currentCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ApiType, Instant> windowStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ApiType, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    public ApiRateLimiter() {
        // 각 API 타입별 초기화
        for (ApiType apiType : ApiType.values()) {
            currentCounts.put(apiType, new AtomicInteger(0));
            windowStartTimes.put(apiType, Instant.now());
            locks.put(apiType, new ReentrantLock());
        }
        
        log.info("API Rate Limiter 초기화 완료 - 카카오톡: {}회/분, SMS: {}회/분", 
            KAKAOTALK_LIMIT_PER_MINUTE, SMS_LIMIT_PER_MINUTE);
    }
    
    /**
     * 특정 API의 호출이 허용되는지 확인하고 허용된다면 카운트를 증가시킵니다.
     * 
     * @param apiType API 타입
     * @return 호출 허용 여부
     */
    public boolean tryAcquire(ApiType apiType) {
        ReentrantLock lock = locks.get(apiType);
        lock.lock();
        
        try {
            Instant now = Instant.now();
            Instant windowStart = windowStartTimes.get(apiType);
            AtomicInteger currentCount = currentCounts.get(apiType);
            
            // 1분이 지났으면 윈도우 리셋
            if (ChronoUnit.MINUTES.between(windowStart, now) >= 1) {
                windowStartTimes.put(apiType, now);
                currentCount.set(0);
                log.debug("{}의 Rate Limit 윈도우 리셋", apiType);
            }
            
            int limit = getLimit(apiType);
            int current = currentCount.get();
            
            if (current < limit) {
                currentCount.incrementAndGet();
                log.debug("{}의 호출 허용: {}/{}", apiType, current + 1, limit);
                return true;
            } else {
                log.warn("{}의 Rate Limit 초과: {}/{} (윈도우 시작: {})", 
                    apiType, current, limit, windowStart);
                return false;
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 현재 API의 사용량 정보를 반환합니다.
     * 
     * @param apiType API 타입
     * @return 사용량 정보
     */
    public RateLimitInfo getCurrentUsage(ApiType apiType) {
        ReentrantLock lock = locks.get(apiType);
        lock.lock();
        
        try {
            Instant now = Instant.now();
            Instant windowStart = windowStartTimes.get(apiType);
            AtomicInteger currentCount = currentCounts.get(apiType);
            
            // 1분이 지났으면 윈도우 리셋
            if (ChronoUnit.MINUTES.between(windowStart, now) >= 1) {
                windowStartTimes.put(apiType, now);
                currentCount.set(0);
                windowStart = now;
            }
            
            int limit = getLimit(apiType);
            int current = currentCount.get();
            long remainingTimeSeconds = 60 - ChronoUnit.SECONDS.between(windowStart, now);
            
            return new RateLimitInfo(
                apiType,
                current,
                limit,
                limit - current,
                windowStart,
                Math.max(0, remainingTimeSeconds)
            );
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 모든 API의 현재 사용량을 로깅합니다.
     */
    public void logCurrentUsage() {
        for (ApiType apiType : ApiType.values()) {
            RateLimitInfo info = getCurrentUsage(apiType);
            log.info("Rate Limit 현황 - {}: {}/{} (남은 시간: {}초)", 
                info.getApiType(), info.getCurrentCount(), info.getLimit(), info.getRemainingTimeSeconds());
        }
    }
    
    private int getLimit(ApiType apiType) {
        switch (apiType) {
            case KAKAOTALK:
                return KAKAOTALK_LIMIT_PER_MINUTE;
            case SMS:
                return SMS_LIMIT_PER_MINUTE;
            default:
                return 100; // 기본값
        }
    }
    
    /**
     * Rate Limit 정보를 담는 클래스
     */
    public static class RateLimitInfo {
        private final ApiType apiType;
        private final int currentCount;
        private final int limit;
        private final int remaining;
        private final Instant windowStart;
        private final long remainingTimeSeconds;
        
        public RateLimitInfo(ApiType apiType, int currentCount, int limit, int remaining, 
                           Instant windowStart, long remainingTimeSeconds) {
            this.apiType = apiType;
            this.currentCount = currentCount;
            this.limit = limit;
            this.remaining = remaining;
            this.windowStart = windowStart;
            this.remainingTimeSeconds = remainingTimeSeconds;
        }
        
        // Getters
        public ApiType getApiType() { return apiType; }
        public int getCurrentCount() { return currentCount; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public Instant getWindowStart() { return windowStart; }
        public long getRemainingTimeSeconds() { return remainingTimeSeconds; }
        
        public boolean isLimitExceeded() {
            return remaining <= 0;
        }
        
        @Override
        public String toString() {
            return String.format("RateLimitInfo{%s: %d/%d, remaining: %d, resetIn: %ds}", 
                apiType, currentCount, limit, remaining, remainingTimeSeconds);
        }
    }
}