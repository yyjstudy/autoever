package com.autoever.member.message.retry;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.exception.MessageApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 재시도 컨텍스트 - 재시도 과정에서의 상태 정보를 관리
 */
public class RetryContext {
    
    private final MessageRequest originalRequest;
    private final RetryPolicy retryPolicy;
    private final LocalDateTime startTime;
    
    private int currentAttempt;
    private ApiType lastUsedApiType;
    private final List<RetryAttempt> attempts;
    private LocalDateTime lastAttemptTime;
    private MessageApiException lastException;
    
    public RetryContext(MessageRequest originalRequest, RetryPolicy retryPolicy) {
        this.originalRequest = originalRequest;
        this.retryPolicy = retryPolicy;
        this.startTime = LocalDateTime.now();
        this.currentAttempt = 0;
        this.attempts = new ArrayList<>();
    }
    
    /**
     * 새로운 시도 시작
     */
    public void startNewAttempt(ApiType apiType) {
        this.currentAttempt++;
        this.lastUsedApiType = apiType;
        this.lastAttemptTime = LocalDateTime.now();
    }
    
    /**
     * 현재 시도 실패 기록
     */
    public void recordFailure(MessageApiException exception, String details) {
        this.lastException = exception;
        
        RetryAttempt attempt = new RetryAttempt(
            currentAttempt,
            lastUsedApiType,
            lastAttemptTime,
            LocalDateTime.now(),
            false,
            exception.getMessage(),
            details
        );
        
        attempts.add(attempt);
    }
    
    /**
     * 현재 시도 성공 기록
     */
    public void recordSuccess(String details) {
        RetryAttempt attempt = new RetryAttempt(
            currentAttempt,
            lastUsedApiType,
            lastAttemptTime,
            LocalDateTime.now(),
            true,
            "성공",
            details
        );
        
        attempts.add(attempt);
    }
    
    /**
     * 다음 재시도 가능 여부 확인
     */
    public boolean canRetry() {
        return retryPolicy.shouldRetry(currentAttempt + 1);
    }
    
    /**
     * 다음 시도까지의 지연 시간 계산
     */
    public java.time.Duration getNextDelay() {
        return retryPolicy.calculateDelay(currentAttempt + 1);
    }
    
    /**
     * 재시도 요약 정보 생성
     */
    public RetrySummary createSummary() {
        boolean finalSuccess = !attempts.isEmpty() && attempts.get(attempts.size() - 1).isSuccess();
        
        return new RetrySummary(
            originalRequest,
            retryPolicy,
            startTime,
            LocalDateTime.now(),
            attempts.size(),
            finalSuccess,
            lastException,
            Collections.unmodifiableList(new ArrayList<>(attempts))
        );
    }
    
    // Getters
    public MessageRequest getOriginalRequest() { return originalRequest; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getCurrentAttempt() { return currentAttempt; }
    public ApiType getLastUsedApiType() { return lastUsedApiType; }
    public List<RetryAttempt> getAttempts() { return Collections.unmodifiableList(attempts); }
    public LocalDateTime getLastAttemptTime() { return lastAttemptTime; }
    public MessageApiException getLastException() { return lastException; }
    
    /**
     * 개별 재시도 시도 정보
     */
    public static class RetryAttempt {
        private final int attemptNumber;
        private final ApiType apiType;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final boolean success;
        private final String resultMessage;
        private final String details;
        
        public RetryAttempt(int attemptNumber, ApiType apiType, LocalDateTime startTime, 
                           LocalDateTime endTime, boolean success, String resultMessage, String details) {
            this.attemptNumber = attemptNumber;
            this.apiType = apiType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.success = success;
            this.resultMessage = resultMessage;
            this.details = details;
        }
        
        public java.time.Duration getDuration() {
            return java.time.Duration.between(startTime, endTime);
        }
        
        // Getters
        public int getAttemptNumber() { return attemptNumber; }
        public ApiType getApiType() { return apiType; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public boolean isSuccess() { return success; }
        public String getResultMessage() { return resultMessage; }
        public String getDetails() { return details; }
        
        @Override
        public String toString() {
            return String.format("Attempt %d (%s): %s - %s [%dms]", 
                attemptNumber, 
                apiType.getDisplayName(), 
                success ? "SUCCESS" : "FAILED", 
                resultMessage,
                getDuration().toMillis()
            );
        }
    }
    
    /**
     * 재시도 전체 요약 정보
     */
    public static class RetrySummary {
        private final MessageRequest originalRequest;
        private final RetryPolicy retryPolicy;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int totalAttempts;
        private final boolean finalSuccess;
        private final MessageApiException lastException;
        private final List<RetryAttempt> attempts;
        
        public RetrySummary(MessageRequest originalRequest, RetryPolicy retryPolicy, 
                           LocalDateTime startTime, LocalDateTime endTime, int totalAttempts, 
                           boolean finalSuccess, MessageApiException lastException, 
                           List<RetryAttempt> attempts) {
            this.originalRequest = originalRequest;
            this.retryPolicy = retryPolicy;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalAttempts = totalAttempts;
            this.finalSuccess = finalSuccess;
            this.lastException = lastException;
            this.attempts = attempts;
        }
        
        public java.time.Duration getTotalDuration() {
            return java.time.Duration.between(startTime, endTime);
        }
        
        public int getSuccessfulAttempts() {
            return (int) attempts.stream().mapToInt(a -> a.isSuccess() ? 1 : 0).sum();
        }
        
        public int getFailedAttempts() {
            return totalAttempts - getSuccessfulAttempts();
        }
        
        // Getters
        public MessageRequest getOriginalRequest() { return originalRequest; }
        public RetryPolicy getRetryPolicy() { return retryPolicy; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalAttempts() { return totalAttempts; }
        public boolean isFinalSuccess() { return finalSuccess; }
        public MessageApiException getLastException() { return lastException; }
        public List<RetryAttempt> getAttempts() { return attempts; }
        
        @Override
        public String toString() {
            return String.format("RetrySummary{attempts=%d, success=%s, duration=%dms, policy=%s}", 
                totalAttempts, finalSuccess, getTotalDuration().toMillis(), retryPolicy);
        }
    }
}