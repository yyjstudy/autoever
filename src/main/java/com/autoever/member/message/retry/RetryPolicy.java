package com.autoever.member.message.retry;

import java.time.Duration;

/**
 * 메시지 발송 재시도 정책
 */
public class RetryPolicy {
    
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final boolean jitterEnabled;
    
    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.jitterEnabled = builder.jitterEnabled;
    }
    
    /**
     * 기본 재시도 정책 반환
     * 최대 3회, 초기 지연 1초, 지수 백오프 2배
     */
    public static RetryPolicy defaultPolicy() {
        return new Builder()
            .maxAttempts(3)
            .initialDelay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofSeconds(30))
            .backoffMultiplier(2.0)
            .withJitter(true)
            .build();
    }
    
    /**
     * 보수적인 재시도 정책 (더 많은 재시도, 긴 지연)
     */
    public static RetryPolicy conservativePolicy() {
        return new Builder()
            .maxAttempts(5)
            .initialDelay(Duration.ofSeconds(2))
            .maxDelay(Duration.ofMinutes(2))
            .backoffMultiplier(1.5)
            .withJitter(true)
            .build();
    }
    
    /**
     * 적극적인 재시도 정책 (빠른 재시도)
     */
    public static RetryPolicy aggressivePolicy() {
        return new Builder()
            .maxAttempts(2)
            .initialDelay(Duration.ofMillis(500))
            .maxDelay(Duration.ofSeconds(10))
            .backoffMultiplier(2.0)
            .withJitter(false)
            .build();
    }
    
    /**
     * 재시도 없음 (즉시 실패)
     */
    public static RetryPolicy noRetryPolicy() {
        return new Builder()
            .maxAttempts(1)
            .build();
    }
    
    /**
     * 특정 시도 번호에 대한 지연 시간 계산
     * 
     * @param attemptNumber 시도 번호 (1부터 시작)
     * @return 지연 시간
     */
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 1) {
            return Duration.ZERO; // 첫 번째 시도는 지연 없음
        }
        
        // 지수 백오프 계산
        double delaySeconds = initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 2) / 1000.0;
        
        // 최대 지연 시간 적용
        delaySeconds = Math.min(delaySeconds, maxDelay.toSeconds());
        
        // 지터(Jitter) 적용 - 동시 재시도로 인한 부하 분산
        if (jitterEnabled) {
            delaySeconds = delaySeconds * (0.5 + Math.random() * 0.5); // 50~100% 범위
        }
        
        return Duration.ofMillis((long) (delaySeconds * 1000));
    }
    
    /**
     * 재시도 가능 여부 확인
     */
    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxAttempts;
    }
    
    // Getters
    public int getMaxAttempts() { return maxAttempts; }
    public Duration getInitialDelay() { return initialDelay; }
    public Duration getMaxDelay() { return maxDelay; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public boolean isJitterEnabled() { return jitterEnabled; }
    
    /**
     * RetryPolicy Builder
     */
    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffMultiplier = 2.0;
        private boolean jitterEnabled = true;
        
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("최대 시도 횟수는 1 이상이어야 합니다");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder initialDelay(Duration initialDelay) {
            if (initialDelay.isNegative()) {
                throw new IllegalArgumentException("초기 지연 시간은 음수일 수 없습니다");
            }
            this.initialDelay = initialDelay;
            return this;
        }
        
        public Builder maxDelay(Duration maxDelay) {
            if (maxDelay.isNegative()) {
                throw new IllegalArgumentException("최대 지연 시간은 음수일 수 없습니다");
            }
            this.maxDelay = maxDelay;
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            if (backoffMultiplier < 1.0) {
                throw new IllegalArgumentException("백오프 배수는 1.0 이상이어야 합니다");
            }
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public Builder withJitter(boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
            return this;
        }
        
        public RetryPolicy build() {
            if (initialDelay.compareTo(maxDelay) > 0) {
                throw new IllegalArgumentException("초기 지연 시간이 최대 지연 시간보다 클 수 없습니다");
            }
            return new RetryPolicy(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("RetryPolicy{maxAttempts=%d, initialDelay=%s, maxDelay=%s, backoffMultiplier=%.1f, jitter=%b}", 
            maxAttempts, initialDelay, maxDelay, backoffMultiplier, jitterEnabled);
    }
}