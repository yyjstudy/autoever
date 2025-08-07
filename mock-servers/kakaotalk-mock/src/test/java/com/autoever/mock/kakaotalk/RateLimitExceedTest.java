package com.autoever.mock.kakaotalk;

import com.autoever.mock.kakaotalk.service.SimpleRateLimiter;
import com.autoever.mock.kakaotalk.service.SimpleRateLimiter.RateLimitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * KakaoTalk Mock 서버 Rate Limit 초과 테스트
 * 실제 Rate Limiting 동작을 검증하는 의미있는 테스트
 */
@DisplayName("KakaoTalk Rate Limit 초과 테스트")
class RateLimitExceedTest {

    private SimpleRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new SimpleRateLimiter();
    }
    
    @Test
    @DisplayName("정확히 100번째 요청까지 성공")
    void testExactLimit() {
        // Given - KakaoTalk 제한: 100회/분
        int limit = 100;
        
        // When & Then - 100번까지는 모두 성공
        for (int i = 1; i <= limit; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed)
                .as("요청 #%d는 성공해야 함", i)
                .isTrue();
        }
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(100);
        assertThat(info.remainingRequests()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("101번째 요청부터 Rate Limit 초과")
    void testRateLimitExceeded() {
        // Given - 100회 제한을 모두 사용
        for (int i = 1; i <= 100; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed).isTrue();
        }
        
        // When & Then - 101번째 요청은 실패해야 함
        boolean exceededRequest = rateLimiter.tryAcquire();
        assertThat(exceededRequest)
            .as("101번째 요청은 Rate Limit 초과로 실패해야 함")
            .isFalse();
        
        // 사용량 확인 - 101회가 기록되어야 함 (초과 요청도 카운트됨)
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(101);
        assertThat(info.remainingRequests()).isEqualTo(0); // 0으로 표시
    }
    
    @Test
    @DisplayName("Rate Limit 초과 후 연속 요청 모두 실패")
    void testContinuousFailureAfterLimit() {
        // Given - 제한 초과까지 요청
        for (int i = 1; i <= 105; i++) {
            rateLimiter.tryAcquire();
        }
        
        // When & Then - 추가 요청들 모두 실패
        for (int i = 1; i <= 5; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed)
                .as("Rate Limit 초과 후 %d번째 추가 요청은 실패해야 함", i)
                .isFalse();
        }
        
        // 최종 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(110); // 105 + 5 추가 요청
        assertThat(info.remainingRequests()).isEqualTo(0); // 0으로 표시
    }
    
    @Test
    @DisplayName("Rate Limit 임계점 테스트")
    void testRateLimitBoundary() {
        // Given - 99번 요청 (임계점 직전)
        for (int i = 1; i <= 99; i++) {
            rateLimiter.tryAcquire();
        }
        
        // When & Then - 100번째 요청은 성공
        boolean hundredth = rateLimiter.tryAcquire();
        assertThat(hundredth)
            .as("100번째 요청은 여전히 허용되어야 함")
            .isTrue();
        
        // When & Then - 101번째 요청은 실패
        boolean oneHundredFirst = rateLimiter.tryAcquire();
        assertThat(oneHundredFirst)
            .as("101번째 요청은 Rate Limit 초과로 실패해야 함")
            .isFalse();
    }
    
    @Test
    @DisplayName("대량 요청 시 Rate Limit 동작")
    void testBulkRequestRateLimit() {
        // Given - 대량 요청 (200회 - 제한의 2배)
        int successCount = 0;
        int failureCount = 0;
        
        // When - 200번 요청
        for (int i = 1; i <= 200; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            if (allowed) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        // Then - 100번 성공, 100번 실패
        assertThat(successCount).isEqualTo(100);
        assertThat(failureCount).isEqualTo(100);
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(200);
        assertThat(info.remainingRequests()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("중간 레벨 요청량 테스트")
    void testMediumVolumeRequests() {
        // Given - 50번 요청 (제한의 50%)
        int requests = 50;
        int successCount = 0;
        
        // When - 50번 모든 요청이 성공해야 함 (KakaoTalk은 100까지 가능)
        for (int i = 1; i <= requests; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            if (allowed) {
                successCount++;
            }
        }
        
        // Then - 50번 모두 성공
        assertThat(successCount).isEqualTo(50);
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(50);
        assertThat(info.remainingRequests()).isEqualTo(50); // 여전히 50회 남음
    }
    
    @Test
    @DisplayName("Rate Limit 정보의 정확성 검증")
    void testRateLimitInfoAccuracy() {
        // Given - 단계별 요청
        int[] checkPoints = {25, 50, 75, 100, 125};
        
        for (int checkPoint : checkPoints) {
            // 체크포인트까지 요청
            while (rateLimiter.getCurrentUsage().currentUsage() < checkPoint) {
                rateLimiter.tryAcquire();
            }
            
            // 정보 정확성 확인
            RateLimitInfo info = rateLimiter.getCurrentUsage();
            assertThat(info.currentUsage()).isEqualTo(checkPoint);
            
            if (checkPoint <= 100) {
                assertThat(info.remainingRequests()).isEqualTo(100 - checkPoint);
            } else {
                assertThat(info.remainingRequests()).isEqualTo(0); // 0으로 표시
            }
            
            assertThat(info.getRemainingTimeSeconds()).isGreaterThan(0);
        }
    }
}