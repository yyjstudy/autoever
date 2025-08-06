package com.autoever.member.message.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * RetryPolicy 단위 테스트
 */
@DisplayName("RetryPolicy 테스트")
class RetryPolicyTest {
    
    @Test
    @DisplayName("기본 재시도 정책 생성 테스트")
    void defaultPolicy() {
        // When
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        // Then
        assertThat(policy.getMaxAttempts()).isEqualTo(3);
        assertThat(policy.getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.getMaxDelay()).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.getBackoffMultiplier()).isEqualTo(2.0);
        assertThat(policy.isJitterEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("보수적인 재시도 정책 생성 테스트")
    void conservativePolicy() {
        // When
        RetryPolicy policy = RetryPolicy.conservativePolicy();
        
        // Then
        assertThat(policy.getMaxAttempts()).isEqualTo(5);
        assertThat(policy.getInitialDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.getMaxDelay()).isEqualTo(Duration.ofMinutes(2));
        assertThat(policy.getBackoffMultiplier()).isEqualTo(1.5);
        assertThat(policy.isJitterEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("적극적인 재시도 정책 생성 테스트")
    void aggressivePolicy() {
        // When
        RetryPolicy policy = RetryPolicy.aggressivePolicy();
        
        // Then
        assertThat(policy.getMaxAttempts()).isEqualTo(2);
        assertThat(policy.getInitialDelay()).isEqualTo(Duration.ofMillis(500));
        assertThat(policy.getMaxDelay()).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.getBackoffMultiplier()).isEqualTo(2.0);
        assertThat(policy.isJitterEnabled()).isFalse();
    }
    
    @Test
    @DisplayName("재시도 없음 정책 테스트")
    void noRetryPolicy() {
        // When
        RetryPolicy policy = RetryPolicy.noRetryPolicy();
        
        // Then
        assertThat(policy.getMaxAttempts()).isEqualTo(1);
        assertThat(policy.shouldRetry(1)).isFalse();
    }
    
    @Test
    @DisplayName("지연 시간 계산 테스트 - 첫 번째 시도")
    void calculateDelay_FirstAttempt() {
        // Given
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        // When
        Duration delay = policy.calculateDelay(1);
        
        // Then
        assertThat(delay).isEqualTo(Duration.ZERO); // 첫 번째 시도는 지연 없음
    }
    
    @Test
    @DisplayName("지연 시간 계산 테스트 - 지수 백오프")
    void calculateDelay_ExponentialBackoff() {
        // Given
        RetryPolicy policy = new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(1))
            .backoffMultiplier(2.0)
            .maxDelay(Duration.ofMinutes(5))
            .withJitter(false) // 지터 비활성화로 정확한 값 테스트
            .build();
        
        // When & Then
        assertThat(policy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(1)); // 1초
        assertThat(policy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(2)); // 2초
        assertThat(policy.calculateDelay(4)).isEqualTo(Duration.ofSeconds(4)); // 4초
        assertThat(policy.calculateDelay(5)).isEqualTo(Duration.ofSeconds(8)); // 8초
    }
    
    @Test
    @DisplayName("최대 지연 시간 제한 테스트")
    void calculateDelay_MaxDelayLimit() {
        // Given
        RetryPolicy policy = new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(10))
            .backoffMultiplier(3.0)
            .maxDelay(Duration.ofSeconds(15)) // 최대 15초
            .withJitter(false)
            .build();
        
        // When
        Duration delay = policy.calculateDelay(5); // 10 * 3^3 = 270초가 되어야 하지만
        
        // Then
        assertThat(delay).isLessThanOrEqualTo(Duration.ofSeconds(15)); // 최대 15초로 제한
    }
    
    @RepeatedTest(10)
    @DisplayName("지터 적용 테스트")
    void calculateDelay_WithJitter() {
        // Given
        RetryPolicy policy = new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(10))
            .backoffMultiplier(1.0) // 배수 1.0으로 고정
            .withJitter(true)
            .build();
        
        // When
        Duration delay = policy.calculateDelay(2);
        
        // Then
        // 지터로 인해 50~100% 범위 (5~10초)
        assertThat(delay.toSeconds()).isBetween(5L, 10L);
    }
    
    @Test
    @DisplayName("재시도 가능 여부 확인 테스트")
    void shouldRetry() {
        // Given
        RetryPolicy policy = new RetryPolicy.Builder()
            .maxAttempts(3)
            .build();
        
        // When & Then
        assertThat(policy.shouldRetry(1)).isTrue();  // 1번 시도 후 재시도 가능
        assertThat(policy.shouldRetry(2)).isTrue();  // 2번 시도 후 재시도 가능
        assertThat(policy.shouldRetry(3)).isFalse(); // 3번 시도 후 재시도 불가
        assertThat(policy.shouldRetry(4)).isFalse(); // 이미 최대 시도 초과
    }
    
    @Test
    @DisplayName("빌더 유효성 검사 테스트 - 최대 시도 횟수")
    void builder_Validation_MaxAttempts() {
        // When & Then
        assertThatThrownBy(() -> new RetryPolicy.Builder().maxAttempts(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 시도 횟수는 1 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("빌더 유효성 검사 테스트 - 초기 지연 시간")
    void builder_Validation_InitialDelay() {
        // When & Then
        assertThatThrownBy(() -> new RetryPolicy.Builder().initialDelay(Duration.ofSeconds(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("초기 지연 시간은 음수일 수 없습니다");
    }
    
    @Test
    @DisplayName("빌더 유효성 검사 테스트 - 백오프 배수")
    void builder_Validation_BackoffMultiplier() {
        // When & Then
        assertThatThrownBy(() -> new RetryPolicy.Builder().backoffMultiplier(0.5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("백오프 배수는 1.0 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("빌더 유효성 검사 테스트 - 초기 지연 > 최대 지연")
    void builder_Validation_InitialDelayGreaterThanMaxDelay() {
        // When & Then
        assertThatThrownBy(() -> new RetryPolicy.Builder()
            .initialDelay(Duration.ofSeconds(30))
            .maxDelay(Duration.ofSeconds(10))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("초기 지연 시간이 최대 지연 시간보다 클 수 없습니다");
    }
    
    @Test
    @DisplayName("커스텀 재시도 정책 생성 테스트")
    void customPolicy_Build() {
        // When
        RetryPolicy policy = new RetryPolicy.Builder()
            .maxAttempts(5)
            .initialDelay(Duration.ofMillis(100))
            .maxDelay(Duration.ofSeconds(60))
            .backoffMultiplier(1.5)
            .withJitter(false)
            .build();
        
        // Then
        assertThat(policy.getMaxAttempts()).isEqualTo(5);
        assertThat(policy.getInitialDelay()).isEqualTo(Duration.ofMillis(100));
        assertThat(policy.getMaxDelay()).isEqualTo(Duration.ofSeconds(60));
        assertThat(policy.getBackoffMultiplier()).isEqualTo(1.5);
        assertThat(policy.isJitterEnabled()).isFalse();
    }
    
    @Test
    @DisplayName("toString 테스트")
    void toStringTest() {
        // Given
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        
        // When
        String result = policy.toString();
        
        // Then
        assertThat(result)
            .contains("RetryPolicy")
            .contains("maxAttempts=3")
            .contains("initialDelay=PT1S")
            .contains("maxDelay=PT30S")
            .contains("backoffMultiplier=2.0")
            .contains("jitter=true");
    }
}