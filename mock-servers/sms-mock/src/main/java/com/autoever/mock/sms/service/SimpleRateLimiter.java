package com.autoever.mock.sms.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 간단한 Rate Limiting 서비스 - 튜토리얼 수준
 * 메모리 기반 카운터로 1분당 요청 횟수 제한
 */
@Service
public class SimpleRateLimiter {
    
    private static final int LIMIT_PER_MINUTE = 500; // SMS: 1분당 500회
    private static final long WINDOW_SIZE_MILLIS = 60_000; // 1분
    
    // 서버 전체 기준 카운터 (IP별 구분 없음)
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile long windowStartTime = System.currentTimeMillis();
    
    /**
     * Rate Limit 체크
     * @return true if allowed, false if rate limited
     */
    public boolean tryAcquire() {
        long currentTime = System.currentTimeMillis();
        
        // 윈도우 리셋 체크 (1분 경과)
        if (currentTime - windowStartTime >= WINDOW_SIZE_MILLIS) {
            resetWindow(currentTime);
        }
        
        // 현재 카운트 증가 후 체크
        int currentCount = requestCount.incrementAndGet();
        return currentCount <= LIMIT_PER_MINUTE;
    }
    
    /**
     * 현재 사용량 정보 반환
     */
    public RateLimitInfo getCurrentUsage() {
        long currentTime = System.currentTimeMillis();
        
        // 윈도우 리셋 체크
        if (currentTime - windowStartTime >= WINDOW_SIZE_MILLIS) {
            resetWindow(currentTime);
        }
        
        int current = requestCount.get();
        int remaining = Math.max(0, LIMIT_PER_MINUTE - current);
        long resetTime = windowStartTime + WINDOW_SIZE_MILLIS;
        
        return new RateLimitInfo(current, remaining, resetTime);
    }
    
    /**
     * 윈도우 리셋 (동기화)
     */
    private synchronized void resetWindow(long currentTime) {
        // Double-check locking
        if (currentTime - windowStartTime >= WINDOW_SIZE_MILLIS) {
            windowStartTime = currentTime;
            requestCount.set(0);
        }
    }
    
    /**
     * Rate Limit 정보를 담는 record 클래스
     */
    public record RateLimitInfo(
        int currentUsage,
        int remainingRequests,
        long resetTimeMillis
    ) {
        public long getRemainingTimeSeconds() {
            return Math.max(0, (resetTimeMillis - System.currentTimeMillis()) / 1000);
        }
    }
}