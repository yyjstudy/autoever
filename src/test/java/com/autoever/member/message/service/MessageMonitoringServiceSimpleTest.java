package com.autoever.member.message.service;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.exception.MessageApiException;
import com.autoever.member.message.retry.RetryContext;
import com.autoever.member.message.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * MessageMonitoringService 단순 테스트
 */
@DisplayName("MessageMonitoringService Simple Test")
class MessageMonitoringServiceSimpleTest {
    
    private MessageMonitoringService monitoringService;
    private MessageRequest messageRequest;
    private RetryPolicy retryPolicy;
    
    @BeforeEach
    void setUp() {
        monitoringService = new MessageMonitoringService();
        messageRequest = new MessageRequest("010-1234-5678", "테스트 메시지");
        retryPolicy = RetryPolicy.defaultPolicy();
    }
    
    @Test
    @DisplayName("초기 통계")
    void initialStatistics() {
        MessageMonitoringService.MonitoringStatistics stats = monitoringService.getCurrentStatistics();
        
        assertThat(stats.getTotalAttempts()).isEqualTo(0);
        assertThat(stats.getTotalSuccesses()).isEqualTo(0);
        assertThat(stats.getTotalFailures()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(0.0);
    }
    
    @Test
    @DisplayName("성공 기록")
    void recordSuccess() {
        RetryContext context = new RetryContext(messageRequest, retryPolicy);
        context.startNewAttempt(ApiType.KAKAOTALK);
        context.recordSuccess("성공");
        
        monitoringService.recordMessageAttempt(context.createSummary());
        
        MessageMonitoringService.MonitoringStatistics stats = monitoringService.getCurrentStatistics();
        assertThat(stats.getTotalAttempts()).isEqualTo(1);
        assertThat(stats.getTotalSuccesses()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("실패 기록")
    void recordFailure() {
        RetryContext context = new RetryContext(messageRequest, retryPolicy);
        context.startNewAttempt(ApiType.SMS);
        context.recordFailure(new MessageApiException(ApiType.SMS, "ERROR", "실패"), "상세");
        
        monitoringService.recordMessageAttempt(context.createSummary());
        
        MessageMonitoringService.MonitoringStatistics stats = monitoringService.getCurrentStatistics();
        assertThat(stats.getTotalAttempts()).isEqualTo(1);
        assertThat(stats.getTotalFailures()).isEqualTo(1);
        assertThat(stats.getFailureRate()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("통계 초기화")
    void resetStatistics() {
        RetryContext context = new RetryContext(messageRequest, retryPolicy);
        context.startNewAttempt(ApiType.KAKAOTALK);
        context.recordSuccess("성공");
        monitoringService.recordMessageAttempt(context.createSummary());
        
        monitoringService.resetStatistics();
        
        MessageMonitoringService.MonitoringStatistics stats = monitoringService.getCurrentStatistics();
        assertThat(stats.getTotalAttempts()).isEqualTo(0);
        assertThat(stats.getTotalSuccesses()).isEqualTo(0);
        assertThat(stats.getTotalFailures()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("전화번호 마스킹")
    void phoneNumberMasking() {
        RetryContext context = new RetryContext(messageRequest, retryPolicy);
        context.startNewAttempt(ApiType.SMS);
        context.recordSuccess("성공");
        
        MessageMonitoringService.MessageAttemptRecord record = 
            new MessageMonitoringService.MessageAttemptRecord(context.createSummary());
        
        assertThat(record.getRecipientPhone()).isEqualTo("010****5678");
    }
}