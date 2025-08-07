package com.autoever.member.message.result;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.queue.MessageQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MessageSendTracker 테스트")
class MessageSendTrackerTest {
    
    private MessageSendTracker tracker;
    
    @BeforeEach
    void setUp() {
        MessageQueueService mockQueueService = mock(MessageQueueService.class);
        when(mockQueueService.getQueueStatus()).thenReturn(new MessageQueueService.QueueStatus(0, 1500));
        tracker = new MessageSendTracker(mockQueueService);
    }
    
    @Test
    @DisplayName("초기 상태 확인")
    void initialState() {
        // when
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        
        // then
        assertThat(stats.totalAttempts()).isEqualTo(0);
        assertThat(stats.successRate()).isEqualTo(0.0);
        assertThat(stats.fallbackRate()).isEqualTo(0.0);
        assertThat(stats.kakaoSuccessCount()).isEqualTo(0);
        assertThat(stats.smsFallbackCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("카카오톡 성공 기록")
    void recordResult_KakaoSuccess() {
        // when
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(1);
        assertThat(stats.kakaoSuccessCount()).isEqualTo(1);
        assertThat(stats.successRate()).isEqualTo(100.0);
        assertThat(stats.fallbackRate()).isEqualTo(0.0);
        assertThat(stats.kakaoAttempts()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("SMS Fallback 성공 기록")
    void recordResult_SmsFallback() {
        // when
        tracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(1);
        assertThat(stats.smsFallbackCount()).isEqualTo(1);
        assertThat(stats.successRate()).isEqualTo(100.0);
        assertThat(stats.fallbackRate()).isEqualTo(100.0);
        assertThat(stats.smsAttempts()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("실패 기록")
    void recordResult_Failed() {
        // when
        tracker.recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(1);
        assertThat(stats.failedCount()).isEqualTo(1);
        assertThat(stats.successRate()).isEqualTo(0.0);
    }
    
    @Test
    @DisplayName("복합 통계 계산")
    void complexStatistics() {
        // when
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
        tracker.recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.KAKAOTALK);
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(5);
        assertThat(stats.successRate()).isEqualTo(60.0); // 3/5 = 60%
        assertThat(stats.fallbackRate()).isEqualTo(20.0); // 1/5 = 20%
        assertThat(stats.kakaoSuccessCount()).isEqualTo(2);
        assertThat(stats.smsFallbackCount()).isEqualTo(1);
        assertThat(stats.failedCount()).isEqualTo(1);
        assertThat(stats.rateLimitedCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("작업별 통계 기록")
    void recordJobResult() {
        // given
        UUID jobId = UUID.randomUUID();
        
        // when
        tracker.recordJobResult(jobId, MessageSendResult.SUCCESS_KAKAO);
        tracker.recordJobResult(jobId, MessageSendResult.SUCCESS_SMS_FALLBACK);
        tracker.recordJobResult(jobId, MessageSendResult.FAILED_BOTH);
        
        // then
        MessageSendTracker.JobStatistics jobStats = tracker.getJobStatistics(jobId);
        assertThat(jobStats).isNotNull();
        assertThat(jobStats.getTotal()).isEqualTo(3);
        assertThat(jobStats.getSuccessCount()).isEqualTo(2);
        assertThat(jobStats.getSuccessRate()).isCloseTo(66.67, within(0.01));
        assertThat(jobStats.getFallbackCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("통계 초기화")
    void reset() {
        // given
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
        
        // when
        tracker.reset();
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(0);
        assertThat(stats.successRate()).isEqualTo(0.0);
        assertThat(stats.kakaoSuccessCount()).isEqualTo(0);
        assertThat(stats.smsFallbackCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("통계 문자열 표현")
    void statisticsToString() {
        // given
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
        
        // when
        String statsString = tracker.getStatistics().toString();
        
        // then
        assertThat(statsString).contains("전체 시도: 2");
        assertThat(statsString).contains("성공률: 100.0%");
        assertThat(statsString).contains("Fallback 비율: 50.0%");
        assertThat(statsString).contains("카카오톡 성공: 1");
        assertThat(statsString).contains("SMS 대체: 1");
    }
}