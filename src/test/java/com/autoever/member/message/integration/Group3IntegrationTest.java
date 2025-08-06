package com.autoever.member.message.integration;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.retry.RetryPolicy;
import com.autoever.member.message.retry.RetryContext;
import com.autoever.member.message.service.MessageMonitoringService;
import com.autoever.member.message.exception.MessageApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Group 3 (재시도 및 모니터링) 통합 테스트
 */
@DisplayName("Group 3 Integration Test")
class Group3IntegrationTest {
    
    @Test
    @DisplayName("RetryPolicy 기본 동작 테스트")
    void retryPolicy_BasicFunctionality() {
        // Given
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        // When & Then
        assertThat(policy.getMaxAttempts()).isEqualTo(3);
        assertThat(policy.shouldRetry(1)).isTrue();
        assertThat(policy.shouldRetry(3)).isFalse();
        assertThat(policy.calculateDelay(1)).isEqualTo(Duration.ZERO);
        assertThat(policy.calculateDelay(2)).isGreaterThan(Duration.ZERO);
    }
    
    @Test
    @DisplayName("RetryContext 기본 동작 테스트")
    void retryContext_BasicFunctionality() {
        // Given
        MessageRequest request = new MessageRequest("010-1234-5678", "테스트");
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        RetryContext context = new RetryContext(request, policy);
        
        // When
        context.startNewAttempt(ApiType.KAKAOTALK);
        context.recordFailure(new MessageApiException(ApiType.KAKAOTALK, "ERROR", "테스트 실패"), "상세 정보");
        
        // Then
        assertThat(context.getCurrentAttempt()).isEqualTo(1);
        assertThat(context.getAttempts()).hasSize(1);
        assertThat(context.canRetry()).isTrue();
        
        RetryContext.RetrySummary summary = context.createSummary();
        assertThat(summary.getTotalAttempts()).isEqualTo(1);
        assertThat(summary.isFinalSuccess()).isFalse();
    }
    
    @Test
    @DisplayName("MessageMonitoringService 기본 동작 테스트")
    void monitoringService_BasicFunctionality() {
        // Given
        MessageMonitoringService service = new MessageMonitoringService();
        MessageRequest request = new MessageRequest("010-1234-5678", "테스트");
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        RetryContext context = new RetryContext(request, policy);
        
        context.startNewAttempt(ApiType.KAKAOTALK);
        context.recordSuccess("성공");
        RetryContext.RetrySummary summary = context.createSummary();
        
        // When
        service.recordMessageAttempt(summary);
        
        // Then
        MessageMonitoringService.MonitoringStatistics stats = service.getCurrentStatistics();
        assertThat(stats.getTotalAttempts()).isEqualTo(1);
        assertThat(stats.getTotalSuccesses()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("지수 백오프 계산 테스트")
    void exponentialBackoff_Calculation() {
        // Given
        RetryPolicy policy = new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(1))
            .backoffMultiplier(2.0)
            .withJitter(false)
            .build();
        
        // When & Then
        assertThat(policy.calculateDelay(1)).isEqualTo(Duration.ZERO);
        assertThat(policy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.calculateDelay(4)).isEqualTo(Duration.ofSeconds(4));
    }
    
    @Test
    @DisplayName("재시도 정책 빌더 검증")
    void retryPolicyBuilder_Validation() {
        // When & Then
        assertThatThrownBy(() -> new RetryPolicy.Builder().maxAttempts(0))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> new RetryPolicy.Builder().backoffMultiplier(0.5))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(30))
            .maxDelay(Duration.ofSeconds(10))
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    @DisplayName("전화번호 마스킹 테스트")
    void phoneNumberMasking() {
        // Given
        MessageMonitoringService service = new MessageMonitoringService();
        MessageRequest request = new MessageRequest("010-1234-5678", "테스트");
        RetryContext context = new RetryContext(request, RetryPolicy.defaultPolicy());
        
        context.startNewAttempt(ApiType.SMS);
        context.recordSuccess("성공");
        
        // When
        MessageMonitoringService.MessageAttemptRecord record = 
            new MessageMonitoringService.MessageAttemptRecord(context.createSummary());
        
        // Then
        assertThat(record.getRecipientPhone()).isEqualTo("010****5678");
    }
    
    @Test
    @DisplayName("다양한 재시도 정책 테스트")
    void differentRetryPolicies() {
        // Given
        RetryPolicy defaultPolicy = RetryPolicy.defaultPolicy();
        RetryPolicy aggressivePolicy = RetryPolicy.aggressivePolicy();
        RetryPolicy conservativePolicy = RetryPolicy.conservativePolicy();
        RetryPolicy noRetryPolicy = RetryPolicy.noRetryPolicy();
        
        // When & Then
        assertThat(defaultPolicy.getMaxAttempts()).isEqualTo(3);
        assertThat(aggressivePolicy.getMaxAttempts()).isEqualTo(2);
        assertThat(conservativePolicy.getMaxAttempts()).isEqualTo(5);
        assertThat(noRetryPolicy.getMaxAttempts()).isEqualTo(1);
        
        assertThat(aggressivePolicy.isJitterEnabled()).isFalse();
        assertThat(defaultPolicy.isJitterEnabled()).isTrue();
    }
}