package com.autoever.mock.sms;

import com.autoever.mock.sms.service.SimpleRateLimiter;
import com.autoever.mock.sms.service.SimpleRateLimiter.RateLimitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * SMS Mock 서버 Rate Limit 초과 테스트
 * 실제 Rate Limiting 동작을 검증하는 의미있는 테스트
 */
@DisplayName("SMS Rate Limit 초과 테스트")
class RateLimitExceedTest {

    private SimpleRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new SimpleRateLimiter();
    }
    
    @Test
    @DisplayName("정확히 500번째 요청까지 성공")
    void testExactLimit() {
        // Given - SMS 제한: 500회/분
        int limit = 500;
        
        // When & Then - 500번까지는 모두 성공
        for (int i = 1; i <= limit; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed)
                .as("요청 #%d는 성공해야 함", i)
                .isTrue();
        }
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(500);
        assertThat(info.remainingRequests()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("501번째 요청부터 Rate Limit 초과")
    void testRateLimitExceeded() {
        // Given - 500회 제한을 모두 사용
        for (int i = 1; i <= 500; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed).isTrue();
        }
        
        // When & Then - 501번째 요청은 실패해야 함
        boolean exceededRequest = rateLimiter.tryAcquire();
        assertThat(exceededRequest)
            .as("501번째 요청은 Rate Limit 초과로 실패해야 함")
            .isFalse();
        
        // 사용량 확인 - 501회가 기록되어야 함 (초과 요청도 카운트됨)
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(501);
        assertThat(info.remainingRequests()).isEqualTo(0); // SMS는 음수가 아닌 0으로 표시
    }
    
    @Test
    @DisplayName("Rate Limit 초과 후 연속 요청 모두 실패")
    void testContinuousFailureAfterLimit() {
        // Given - 제한 초과까지 요청 (505회)
        for (int i = 1; i <= 505; i++) {
            rateLimiter.tryAcquire();
        }
        
        // When & Then - 추가 요청들 모두 실패
        for (int i = 1; i <= 10; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            assertThat(allowed)
                .as("Rate Limit 초과 후 %d번째 추가 요청은 실패해야 함", i)
                .isFalse();
        }
        
        // 최종 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(515); // 505 + 10 추가 요청
        assertThat(info.remainingRequests()).isEqualTo(0); // SMS는 0으로 표시
    }
    
    @Test
    @DisplayName("Rate Limit 임계점 테스트")
    void testRateLimitBoundary() {
        // Given - 499번 요청 (임계점 직전)
        for (int i = 1; i <= 499; i++) {
            rateLimiter.tryAcquire();
        }
        
        // When & Then - 500번째 요청은 성공
        boolean fiveHundredth = rateLimiter.tryAcquire();
        assertThat(fiveHundredth)
            .as("500번째 요청은 여전히 허용되어야 함")
            .isTrue();
        
        // When & Then - 501번째 요청은 실패
        boolean fiveHundredFirst = rateLimiter.tryAcquire();
        assertThat(fiveHundredFirst)
            .as("501번째 요청은 Rate Limit 초과로 실패해야 함")
            .isFalse();
    }
    
    @Test
    @DisplayName("대량 요청 시 Rate Limit 동작")
    void testBulkRequestRateLimit() {
        // Given - 대량 요청 (1000회 - 제한의 2배)
        int successCount = 0;
        int failureCount = 0;
        
        // When - 1000번 요청
        for (int i = 1; i <= 1000; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            if (allowed) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        // Then - 500번 성공, 500번 실패
        assertThat(successCount).isEqualTo(500);
        assertThat(failureCount).isEqualTo(500);
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(1000);
        assertThat(info.remainingRequests()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("중간 레벨 요청량 테스트")
    void testMediumVolumeRequests() {
        // Given - 250번 요청 (제한의 50%)
        int requests = 250;
        int successCount = 0;
        
        // When - 250번 모든 요청이 성공해야 함 (SMS는 500까지 가능)
        for (int i = 1; i <= requests; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            if (allowed) {
                successCount++;
            }
        }
        
        // Then - 250번 모두 성공
        assertThat(successCount).isEqualTo(250);
        
        // 사용량 확인
        RateLimitInfo info = rateLimiter.getCurrentUsage();
        assertThat(info.currentUsage()).isEqualTo(250);
        assertThat(info.remainingRequests()).isEqualTo(250); // 여전히 250회 남음
    }
    
    @Test
    @DisplayName("Rate Limit 정보의 정확성 검증")
    void testRateLimitInfoAccuracy() {
        // Given - 단계별 요청 (SMS 제한에 맞게 조정)
        int[] checkPoints = {125, 250, 375, 500, 625};
        
        for (int checkPoint : checkPoints) {
            // 체크포인트까지 요청
            while (rateLimiter.getCurrentUsage().currentUsage() < checkPoint) {
                rateLimiter.tryAcquire();
            }
            
            // 정보 정확성 확인
            RateLimitInfo info = rateLimiter.getCurrentUsage();
            assertThat(info.currentUsage()).isEqualTo(checkPoint);
            
            if (checkPoint <= 500) {
                assertThat(info.remainingRequests()).isEqualTo(500 - checkPoint);
            } else {
                assertThat(info.remainingRequests()).isEqualTo(0); // SMS는 0으로 표시
            }
            
            assertThat(info.getRemainingTimeSeconds()).isGreaterThan(0);
        }
    }
}