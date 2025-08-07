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
        assertThat(stats.kakaoSuccessCount()).isEqualTo(0);
        assertThat(stats.smsSuccessCount()).isEqualTo(0);
        assertThat(stats.failureCount()).isEqualTo(0);
        assertThat(stats.currentQueueSize()).isEqualTo(0);
        assertThat(stats.maxQueueSize()).isEqualTo(1500);
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
        assertThat(stats.smsSuccessCount()).isEqualTo(0);
        assertThat(stats.failureCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("복합 통계 계산")
    void complexStatistics() {
        // when - 카카오톡 2번 성공, 2번 실패 (FAILED_BOTH, RATE_LIMITED)
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
        tracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.KAKAOTALK);
        
        // SMS 1번 성공, 1번 실패 (FAILED_BOTH만)
        tracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
        
        // then
        MessageSendTracker.SendStatistics stats = tracker.getStatistics();
        assertThat(stats.totalAttempts()).isEqualTo(5);
        assertThat(stats.kakaoSuccessCount()).isEqualTo(2);
        assertThat(stats.smsSuccessCount()).isEqualTo(1);
        assertThat(stats.failureCount()).isEqualTo(2); // FAILED_BOTH + RATE_LIMITED = 2
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
        assertThat(stats.kakaoSuccessCount()).isEqualTo(0);
        assertThat(stats.smsSuccessCount()).isEqualTo(0);
        assertThat(stats.failureCount()).isEqualTo(0);
    }
}