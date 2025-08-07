package com.autoever.member.message.result;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.queue.MessageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메시지 발송 결과 추적 시스템
 * 발송 성공률, Fallback 비율 등의 통계를 실시간으로 추적합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendTracker {
    
    private final MessageQueueService messageQueueService;
    
    // 결과별 카운터
    private final Map<MessageSendResult, AtomicInteger> resultCounters = new ConcurrentHashMap<>();
    
    // API 타입별 발송 시도 카운터
    private final Map<ApiType, AtomicInteger> apiAttemptCounters = new ConcurrentHashMap<>();
    
    // 전체 발송 시도 수
    private final AtomicLong totalAttempts = new AtomicLong(0);
    
    // 통계 시작 시간
    private final LocalDateTime startTime = LocalDateTime.now();
    
    // 진행 중인 작업별 통계
    private final Map<UUID, JobStatistics> jobStatistics = new ConcurrentHashMap<>();
    
    {
        // 모든 결과 타입에 대한 카운터 초기화
        for (MessageSendResult result : MessageSendResult.values()) {
            resultCounters.put(result, new AtomicInteger(0));
        }
        
        // API 타입별 카운터 초기화
        for (ApiType apiType : ApiType.values()) {
            apiAttemptCounters.put(apiType, new AtomicInteger(0));
        }
    }
    
    /**
     * 메시지 발송 결과를 기록합니다.
     * 
     * @param result 발송 결과
     * @param apiType 시도한 API 타입
     */
    public void recordResult(MessageSendResult result, ApiType apiType) {
        resultCounters.get(result).incrementAndGet();
        apiAttemptCounters.get(apiType).incrementAndGet();
        totalAttempts.incrementAndGet();
        
        log.debug("메시지 발송 결과 기록 - 결과: {}, API: {}", result, apiType);
    }
    
    /**
     * 특정 작업의 발송 결과를 기록합니다.
     * 
     * @param jobId 작업 ID
     * @param result 발송 결과
     */
    public void recordJobResult(UUID jobId, MessageSendResult result) {
        JobStatistics stats = jobStatistics.computeIfAbsent(jobId, k -> new JobStatistics());
        stats.recordResult(result);
        
        // 전체 통계에도 반영
        resultCounters.get(result).incrementAndGet();
        totalAttempts.incrementAndGet();
    }
    
    
    /**
     * 현재 통계 정보를 반환합니다.
     * 
     * @return 통계 정보
     */
    public SendStatistics getStatistics() {
        MessageQueueService.QueueStatus queueStatus = messageQueueService.getQueueStatus();
        
        // 성공 카운트
        int kakaoSuccessCount = resultCounters.get(MessageSendResult.SUCCESS_KAKAO).get();
        int smsSuccessCount = resultCounters.get(MessageSendResult.SUCCESS_SMS_FALLBACK).get();
        
        // 실패 카운트 계산 (카카오톡과 SMS 실패를 합산)
        int failureCount = resultCounters.get(MessageSendResult.FAILED_BOTH).get() +
                          resultCounters.get(MessageSendResult.RATE_LIMITED).get() +
                          resultCounters.get(MessageSendResult.QUEUE_FULL).get() +
                          resultCounters.get(MessageSendResult.INVALID_RECIPIENT).get();
        
        return new SendStatistics(
            totalAttempts.get(),
            kakaoSuccessCount,
            smsSuccessCount,
            failureCount,
            queueStatus.getCurrentSize(),
            queueStatus.getMaxSize()
        );
    }
    
    /**
     * 특정 작업의 통계를 반환합니다.
     * 
     * @param jobId 작업 ID
     * @return 작업 통계 (없으면 null)
     */
    public JobStatistics getJobStatistics(UUID jobId) {
        return jobStatistics.get(jobId);
    }
    
    /**
     * 통계를 초기화합니다.
     */
    public void reset() {
        resultCounters.values().forEach(counter -> counter.set(0));
        apiAttemptCounters.values().forEach(counter -> counter.set(0));
        totalAttempts.set(0);
        jobStatistics.clear();
        
        log.info("메시지 발송 통계 초기화 완료");
    }
    
    /**
     * 발송 통계 정보
     */
    public record SendStatistics(
        long totalAttempts,
        int kakaoSuccessCount,
        int smsSuccessCount,
        int failureCount,
        int currentQueueSize,
        int maxQueueSize
    ) {
        @Override
        public String toString() {
            return String.format(
                "전체 시도: %d, 카카오톡 성공: %d, SMS 성공: %d, " +
                "실패: %d, 큐상태: %d/%d",
                totalAttempts, kakaoSuccessCount, smsSuccessCount,
                failureCount, currentQueueSize, maxQueueSize
            );
        }
    }
    
    /**
     * 작업별 통계
     */
    public static class JobStatistics {
        private final Map<MessageSendResult, AtomicInteger> resultCounts = new ConcurrentHashMap<>();
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private final LocalDateTime startTime = LocalDateTime.now();
        
        public JobStatistics() {
            for (MessageSendResult result : MessageSendResult.values()) {
                resultCounts.put(result, new AtomicInteger(0));
            }
        }
        
        public void recordResult(MessageSendResult result) {
            resultCounts.get(result).incrementAndGet();
            totalCount.incrementAndGet();
        }
        
        public int getTotal() {
            return totalCount.get();
        }
        
        public int getSuccessCount() {
            return resultCounts.get(MessageSendResult.SUCCESS_KAKAO).get() +
                   resultCounts.get(MessageSendResult.SUCCESS_SMS_FALLBACK).get();
        }
        
        public double getSuccessRate() {
            int total = totalCount.get();
            return total == 0 ? 0.0 : (double) getSuccessCount() / total * 100;
        }
        
        public int getFallbackCount() {
            return resultCounts.get(MessageSendResult.SUCCESS_SMS_FALLBACK).get();
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
    }
}