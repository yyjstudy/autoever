package com.autoever.member.message.ratelimit;

import com.autoever.member.message.ApiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * RateLimiter 단위 테스트
 */
@DisplayName("RateLimiter 테스트")
class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }
    
    @Test
    @DisplayName("Rate Limiter 초기화 테스트")
    void initialization() {
        // When & Then
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(100);
        assertThat(rateLimiter.getAvailableTokens(ApiType.SMS)).isEqualTo(500);
    }
    
    @Test
    @DisplayName("토큰 획득 성공 테스트")
    void tryAcquire_Success() {
        // When
        boolean acquired = rateLimiter.tryAcquire(ApiType.KAKAOTALK);
        
        // Then
        assertThat(acquired).isTrue();
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(99);
    }
    
    @Test
    @DisplayName("여러 토큰 동시 획득 테스트")
    void tryAcquire_MultipleTokens() {
        // When
        boolean acquired = rateLimiter.tryAcquire(ApiType.KAKAOTALK, 10);
        
        // Then
        assertThat(acquired).isTrue();
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(90);
    }
    
    @Test
    @DisplayName("토큰 부족 시 획득 실패 테스트")
    void tryAcquire_InsufficientTokens() {
        // Given - 모든 토큰 소진
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 100);
        
        // When
        boolean acquired = rateLimiter.tryAcquire(ApiType.KAKAOTALK);
        
        // Then
        assertThat(acquired).isFalse();
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(0);
    }
    
    @Test
    @DisplayName("서로 다른 API 타입은 독립적인 토큰 관리")
    void tryAcquire_DifferentApiTypes_IndependentTokens() {
        // When
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 100); // 카카오톡 토큰 모두 소진
        boolean smsAcquired = rateLimiter.tryAcquire(ApiType.SMS);
        
        // Then
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(0);
        assertThat(smsAcquired).isTrue();
        assertThat(rateLimiter.getAvailableTokens(ApiType.SMS)).isEqualTo(499);
    }
    
    @Test
    @DisplayName("타임아웃과 함께 토큰 획득 테스트 - 즉시 성공")
    void tryAcquireWithTimeout_ImmediateSuccess() {
        // When
        boolean acquired = rateLimiter.tryAcquire(ApiType.KAKAOTALK, 1000, TimeUnit.MILLISECONDS);
        
        // Then
        assertThat(acquired).isTrue();
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(99);
    }
    
    @Test
    @DisplayName("타임아웃과 함께 토큰 획득 테스트 - 토큰 부족으로 실패")
    void tryAcquireWithTimeout_Timeout() {
        // Given - 모든 토큰 소진
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 100);
        
        // When
        long startTime = System.currentTimeMillis();
        boolean acquired = rateLimiter.tryAcquire(ApiType.KAKAOTALK, 200, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(acquired).isFalse();
        assertThat(duration).isGreaterThanOrEqualTo(200); // 최소 200ms 대기
        assertThat(duration).isLessThan(300); // 하지만 300ms는 넘지 않음
    }
    
    @Test
    @DisplayName("토큰 부족 시 남은 시간 계산 테스트")
    void getRemainingTime_TokensAvailable() {
        // When - 토큰이 있는 경우
        Duration remaining = rateLimiter.getRemainingTime(ApiType.KAKAOTALK);
        
        // Then
        assertThat(remaining).isEqualTo(Duration.ZERO);
    }
    
    @Test
    @DisplayName("토큰 소진 시 남은 시간 계산 테스트")
    void getRemainingTime_NoTokens() {
        // Given - 모든 토큰 소진
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 100);
        
        // When
        Duration remaining = rateLimiter.getRemainingTime(ApiType.KAKAOTALK);
        
        // Then
        assertThat(remaining.getSeconds()).isGreaterThan(0); // 대기 시간이 있음
    }
    
    @Test
    @DisplayName("버킷 리셋 테스트")
    void resetBucket() {
        // Given - 토큰 일부 소진
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 50);
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(50);
        
        // When
        rateLimiter.resetBucket(ApiType.KAKAOTALK);
        
        // Then
        assertThat(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).isEqualTo(100); // 초기 상태로 복원
    }
    
    @Test
    @DisplayName("Rate Limiter 상태 조회 테스트")
    void getStatus() {
        // Given - 일부 토큰 소진
        rateLimiter.tryAcquire(ApiType.KAKAOTALK, 20);
        rateLimiter.tryAcquire(ApiType.SMS, 100);
        
        // When
        RateLimiter.RateLimiterStatus status = rateLimiter.getStatus();
        
        // Then
        assertThat(status).isNotNull();
        assertThat(status.getTimestamp()).isNotNull();
        assertThat(status.getBuckets()).hasSize(2); // KAKAOTALK, SMS 두 개
        
        RateLimiter.TokenBucketStatus kakaoStatus = status.getBuckets().get(ApiType.KAKAOTALK);
        assertThat(kakaoStatus.getAvailableTokens()).isEqualTo(80);
        assertThat(kakaoStatus.getCapacity()).isEqualTo(100);
        assertThat(kakaoStatus.getRefillRate()).isEqualTo(100);
        assertThat(kakaoStatus.getUsageRate()).isEqualTo(0.2); // 20% 사용
        
        RateLimiter.TokenBucketStatus smsStatus = status.getBuckets().get(ApiType.SMS);
        assertThat(smsStatus.getAvailableTokens()).isEqualTo(400);
        assertThat(smsStatus.getCapacity()).isEqualTo(500);
        assertThat(smsStatus.getUsageRate()).isEqualTo(0.2); // 20% 사용
    }
    
    @Test
    @DisplayName("존재하지 않는 API 타입도 자동으로 버킷 생성")
    void getAvailableTokens_AutoCreateBucket() {
        // When & Then - 모든 ApiType enum 값에 대해 토큰 조회 가능
        for (ApiType apiType : ApiType.values()) {
            long tokens = rateLimiter.getAvailableTokens(apiType);
            assertThat(tokens).isEqualTo(apiType.getRateLimit());
        }
    }
}