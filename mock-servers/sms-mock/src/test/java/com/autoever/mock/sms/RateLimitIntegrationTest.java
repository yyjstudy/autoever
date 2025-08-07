package com.autoever.mock.sms;

import com.autoever.mock.sms.service.SimpleRateLimiter;
import com.autoever.mock.sms.service.SimpleRateLimiter.RateLimitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * SMS Mock 서버 Rate Limiting 단위 테스트
 * 튜토리얼 수준의 기본적인 테스트
 */
@DisplayName("SMS Mock Rate Limiting 테스트")
class RateLimitIntegrationTest {

    private SimpleRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new SimpleRateLimiter();
    }
    
    @Test
    @DisplayName("정상 요청 - Rate Limit 내에서 성공")
    void testNormalRequest() {
        // When
        boolean allowed = rateLimiter.tryAcquire();
        
        // Then
        assertThat(allowed).isTrue();
    }
    
    @Test
    @DisplayName("Rate Limit 정보 확인 - SMS는 500회/분")
    void testRateLimitInfo() {
        // When
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        
        // Then
        assertThat(info.currentUsage()).isEqualTo(0);
        assertThat(info.remainingRequests()).isEqualTo(500); // SMS 제한
        assertThat(info.resetTimeMillis()).isGreaterThan(System.currentTimeMillis());
    }
    
    @Test
    @DisplayName("연속 요청 테스트 - SMS는 더 많은 제한(500회/분)")
    void testSequentialRequests() {
        // When & Then - 첫 번째 요청
        boolean first = rateLimiter.tryAcquire();
        assertThat(first).isTrue();
        
        // When & Then - 두 번째 요청
        boolean second = rateLimiter.tryAcquire();
        assertThat(second).isTrue();
        
        // 사용량 정보 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(2);
        assertThat(info.remainingRequests()).isEqualTo(498);
    }
    
    @Test
    @DisplayName("다중 요청 테스트 - SMS의 높은 제한 확인")
    void testMultipleRequests() {
        // Given - 50번 요청 (KakaoTalk보다 더 많이)
        int requests = 50;
        
        // When
        for (int i = 0; i < requests; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed).isTrue();
        }
        
        // Then
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(50);
        assertThat(info.remainingRequests()).isEqualTo(450); // SMS는 500 제한
    }
    
    @Test
    @DisplayName("KakaoTalk vs SMS 제한 차이 확인")
    void testHigherLimitThanKakaoTalk() {
        // Given - KakaoTalk 제한(100)보다 많은 150번 요청
        int requests = 150;
        
        // When
        for (int i = 0; i < requests; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed).isTrue(); // SMS는 500까지 가능하므로 모두 성공
        }
        
        // Then
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(150);
        assertThat(info.remainingRequests()).isEqualTo(350);
    }
}