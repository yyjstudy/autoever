package com.autoever.member.message.ratelimit;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket 구현체
 * 지정된 속도로 토큰을 리필하고, 토큰 소비를 통해 요청 속도를 제한합니다.
 */
public class TokenBucket {
    
    private final long capacity;        // 최대 토큰 수
    private final long refillRate;      // 분당 리필되는 토큰 수
    private volatile long tokens;       // 현재 토큰 수
    private volatile LocalDateTime lastRefillTime;  // 마지막 리필 시간
    private final ReentrantLock lock = new ReentrantLock();
    
    public TokenBucket(long capacity, long refillRatePerMinute) {
        this.capacity = capacity;
        this.refillRate = refillRatePerMinute;
        this.tokens = capacity; // 초기에는 최대 용량으로 설정
        this.lastRefillTime = LocalDateTime.now();
    }
    
    /**
     * 토큰 소비 시도
     * 
     * @param tokensToConsume 소비할 토큰 수
     * @return 토큰 소비 성공 여부
     */
    public boolean tryConsume(long tokensToConsume) {
        lock.lock();
        try {
            refill(); // 먼저 토큰 리필
            
            if (tokens >= tokensToConsume) {
                tokens -= tokensToConsume;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 현재 시간 기준으로 토큰 리필
     */
    public void refill() {
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            long secondsSinceLastRefill = ChronoUnit.SECONDS.between(lastRefillTime, now);
            
            if (secondsSinceLastRefill > 0) {
                // 분당 리필 속도를 초당으로 변환하여 계산
                long tokensToAdd = (secondsSinceLastRefill * refillRate) / 60;
                
                if (tokensToAdd > 0) {
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefillTime = now;
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 현재 사용 가능한 토큰 수 반환
     */
    public long getAvailableTokens() {
        lock.lock();
        try {
            refill(); // 최신 상태로 업데이트
            return tokens;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 최대 용량 반환
     */
    public long getCapacity() {
        return capacity;
    }
    
    /**
     * 토큰이 가득 찬 상태인지 확인
     */
    public boolean isFull() {
        return getAvailableTokens() >= capacity;
    }
    
    /**
     * 토큰이 비어있는 상태인지 확인
     */
    public boolean isEmpty() {
        return getAvailableTokens() == 0;
    }
    
    /**
     * 분당 리필 속도 반환
     */
    public long getRefillRate() {
        return refillRate;
    }
    
    /**
     * 마지막 리필 시간 반환
     */
    public LocalDateTime getLastRefillTime() {
        return lastRefillTime;
    }
}