package com.autoever.member.message.ratelimit;

import com.autoever.member.message.ApiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API 타입별 Rate Limiting 관리자
 * Token Bucket 알고리즘을 사용하여 API 호출 속도를 제한합니다.
 */
@Component
public class RateLimiter {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    
    private final ConcurrentHashMap<ApiType, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    public RateLimiter() {
        initializeBuckets();
    }
    
    /**
     * API 타입별 Token Bucket 초기화
     */
    private void initializeBuckets() {
        for (ApiType apiType : ApiType.values()) {
            TokenBucket bucket = new TokenBucket(
                apiType.getRateLimit(), // 최대 용량 = 분당 제한 수
                apiType.getRateLimit()  // 리필 속도 = 분당 제한 수
            );
            buckets.put(apiType, bucket);
            
            log.info("Rate Limiter 초기화: {} - 용량: {}, 분당 리필: {}", 
                apiType.getDisplayName(), apiType.getRateLimit(), apiType.getRateLimit());
        }
    }
    
    /**
     * 토큰 획득 시도 (즉시 반환)
     * 
     * @param apiType API 타입
     * @return 토큰 획득 성공 여부
     */
    public boolean tryAcquire(ApiType apiType) {
        return tryAcquire(apiType, 1);
    }
    
    /**
     * 지정된 수의 토큰 획득 시도 (즉시 반환)
     * 
     * @param apiType API 타입
     * @param tokens 획득할 토큰 수
     * @return 토큰 획득 성공 여부
     */
    public boolean tryAcquire(ApiType apiType, long tokens) {
        TokenBucket bucket = getBucket(apiType);
        boolean acquired = bucket.tryConsume(tokens);
        
        if (acquired) {
            log.debug("Rate Limit 토큰 획득 성공: {} - 토큰 {}개, 남은 토큰: {}", 
                apiType.getDisplayName(), tokens, bucket.getAvailableTokens());
        } else {
            log.warn("Rate Limit 제한 도달: {} - 요청 토큰: {}개, 사용 가능한 토큰: {}", 
                apiType.getDisplayName(), tokens, bucket.getAvailableTokens());
        }
        
        return acquired;
    }
    
    /**
     * 토큰 획득 시도 (타임아웃 지원)
     * 
     * @param apiType API 타입
     * @param timeout 대기 시간
     * @param timeUnit 시간 단위
     * @return 토큰 획득 성공 여부
     */
    public boolean tryAcquire(ApiType apiType, long timeout, TimeUnit timeUnit) {
        long timeoutMillis = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (tryAcquire(apiType)) {
                return true;
            }
            
            // 짧은 간격으로 재시도
            try {
                Thread.sleep(Math.min(100, timeoutMillis / 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate Limiter 대기 중 인터럽트 발생: {}", apiType.getDisplayName());
                return false;
            }
        }
        
        log.warn("Rate Limiter 타임아웃: {} - 대기시간: {}ms", 
            apiType.getDisplayName(), timeoutMillis);
        return false;
    }
    
    /**
     * 현재 사용 가능한 토큰 수 반환
     */
    public long getAvailableTokens(ApiType apiType) {
        return getBucket(apiType).getAvailableTokens();
    }
    
    /**
     * 토큰이 리필될 때까지의 예상 시간 반환
     */
    public Duration getRemainingTime(ApiType apiType) {
        TokenBucket bucket = getBucket(apiType);
        
        if (bucket.getAvailableTokens() > 0) {
            return Duration.ZERO; // 토큰이 있으면 대기 시간 없음
        }
        
        // 토큰이 1개 리필될 때까지의 시간 계산 (분당 리필 속도 기준)
        double secondsPerToken = 60.0 / bucket.getRefillRate();
        return Duration.of((long) Math.ceil(secondsPerToken), ChronoUnit.SECONDS);
    }
    
    /**
     * 특정 API 타입의 Token Bucket 리셋
     */
    public void resetBucket(ApiType apiType) {
        TokenBucket newBucket = new TokenBucket(
            apiType.getRateLimit(),
            apiType.getRateLimit()
        );
        buckets.put(apiType, newBucket);
        
        log.info("Rate Limiter 버킷 리셋: {} - 용량: {}", 
            apiType.getDisplayName(), apiType.getRateLimit());
    }
    
    /**
     * API 타입에 해당하는 Token Bucket 반환
     */
    private TokenBucket getBucket(ApiType apiType) {
        return buckets.computeIfAbsent(apiType, type -> {
            log.warn("Token Bucket이 존재하지 않음. 새로 생성: {}", type.getDisplayName());
            return new TokenBucket(type.getRateLimit(), type.getRateLimit());
        });
    }
    
    /**
     * Rate Limiter 상태 정보 반환 (모니터링용)
     */
    public RateLimiterStatus getStatus() {
        ConcurrentHashMap<ApiType, TokenBucketStatus> bucketStatuses = new ConcurrentHashMap<>();
        
        for (ApiType apiType : ApiType.values()) {
            TokenBucket bucket = getBucket(apiType);
            bucketStatuses.put(apiType, new TokenBucketStatus(
                bucket.getAvailableTokens(),
                bucket.getCapacity(),
                bucket.getRefillRate(),
                bucket.getLastRefillTime()
            ));
        }
        
        return new RateLimiterStatus(bucketStatuses);
    }
    
    /**
     * Token Bucket 상태 정보
     */
    public static class TokenBucketStatus {
        private final long availableTokens;
        private final long capacity;
        private final long refillRate;
        private final LocalDateTime lastRefillTime;
        
        public TokenBucketStatus(long availableTokens, long capacity, long refillRate, LocalDateTime lastRefillTime) {
            this.availableTokens = availableTokens;
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.lastRefillTime = lastRefillTime;
        }
        
        public long getAvailableTokens() { return availableTokens; }
        public long getCapacity() { return capacity; }
        public long getRefillRate() { return refillRate; }
        public LocalDateTime getLastRefillTime() { return lastRefillTime; }
        public double getUsageRate() { 
            return capacity > 0 ? (double)(capacity - availableTokens) / capacity : 0.0; 
        }
    }
    
    /**
     * Rate Limiter 전체 상태 정보
     */
    public static class RateLimiterStatus {
        private final ConcurrentHashMap<ApiType, TokenBucketStatus> buckets;
        private final LocalDateTime timestamp;
        
        public RateLimiterStatus(ConcurrentHashMap<ApiType, TokenBucketStatus> buckets) {
            this.buckets = buckets;
            this.timestamp = LocalDateTime.now();
        }
        
        public ConcurrentHashMap<ApiType, TokenBucketStatus> getBuckets() { return buckets; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}