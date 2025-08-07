package com.autoever.mock.kakaotalk;

import com.autoever.mock.kakaotalk.service.SimpleRateLimiter;
import com.autoever.mock.kakaotalk.service.SimpleRateLimiter.RateLimitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * KakaoTalk Mock 서버 Rate Limiting 단위 테스트
 * 튜토리얼 수준의 기본적인 테스트
 */
@DisplayName("KakaoTalk Mock Rate Limiting 테스트")
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
    @DisplayName("Rate Limit 정보 확인")
    void testRateLimitInfo() {
        // When
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        
        // Then
        assertThat(info.currentUsage()).isEqualTo(0);
        assertThat(info.remainingRequests()).isEqualTo(100); // KakaoTalk 제한
        assertThat(info.resetTimeMillis()).isGreaterThan(System.currentTimeMillis());
    }
    
    @Test
    @DisplayName("연속 요청 테스트 - 카운터 증가 확인")
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
        assertThat(info.remainingRequests()).isEqualTo(98);
    }
    
    @Test
    @DisplayName("다중 요청 테스트 - 카운터 정확성")
    void testMultipleRequests() {
        // Given - 10번 요청
        int requests = 10;
        
        // When
        for (int i = 0; i < requests; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed).isTrue();
        }
        
        // Then
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(10);
        assertThat(info.remainingRequests()).isEqualTo(90);
    }
}