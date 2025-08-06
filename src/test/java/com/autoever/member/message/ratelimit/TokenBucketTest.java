package com.autoever.member.message.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * TokenBucket 단위 테스트
 */
@DisplayName("TokenBucket 테스트")
class TokenBucketTest {
    
    @Test
    @DisplayName("토큰 버킷 초기화 테스트")
    void initialization() {
        // Given & When
        TokenBucket bucket = new TokenBucket(100, 60); // 용량 100, 분당 60개 리필
        
        // Then
        assertThat(bucket.getCapacity()).isEqualTo(100);
        assertThat(bucket.getRefillRate()).isEqualTo(60);
        assertThat(bucket.getAvailableTokens()).isEqualTo(100); // 초기에는 가득 참
        assertThat(bucket.isFull()).isTrue();
        assertThat(bucket.isEmpty()).isFalse();
        assertThat(bucket.getLastRefillTime()).isNotNull();
    }
    
    @Test
    @DisplayName("토큰 소비 성공 테스트")
    void tryConsume_Success() {
        // Given
        TokenBucket bucket = new TokenBucket(100, 60);
        
        // When
        boolean consumed = bucket.tryConsume(10);
        
        // Then
        assertThat(consumed).isTrue();
        assertThat(bucket.getAvailableTokens()).isEqualTo(90);
    }
    
    @Test
    @DisplayName("토큰 부족 시 소비 실패 테스트")
    void tryConsume_InsufficientTokens() {
        // Given
        TokenBucket bucket = new TokenBucket(10, 60);
        
        // When
        boolean consumed = bucket.tryConsume(15); // 용량보다 많은 토큰 요청
        
        // Then
        assertThat(consumed).isFalse();
        assertThat(bucket.getAvailableTokens()).isEqualTo(10); // 토큰 수 변화 없음
    }
    
    @Test
    @DisplayName("토큰 모두 소비 후 빈 상태 확인")
    void consume_AllTokens_BecomesEmpty() {
        // Given
        TokenBucket bucket = new TokenBucket(5, 60);
        
        // When
        boolean consumed = bucket.tryConsume(5);
        
        // Then
        assertThat(consumed).isTrue();
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        assertThat(bucket.isEmpty()).isTrue();
        assertThat(bucket.isFull()).isFalse();
    }
    
    @Test
    @DisplayName("토큰 리필 테스트")
    void refill_AddsTokens() throws InterruptedException {
        // Given
        TokenBucket bucket = new TokenBucket(60, 60); // 분당 60개 = 초당 1개
        bucket.tryConsume(60); // 모든 토큰 소비
        
        // When
        Thread.sleep(2000); // 2초 대기
        bucket.refill(); // 명시적 리필
        
        // Then
        assertThat(bucket.getAvailableTokens()).isGreaterThan(0); // 토큰이 리필됨
        assertThat(bucket.getAvailableTokens()).isLessThanOrEqualTo(2); // 최대 2개까지 리필
    }
    
    @Test
    @DisplayName("토큰 리필 시 용량 초과하지 않음 테스트")
    void refill_DoesNotExceedCapacity() throws InterruptedException {
        // Given
        TokenBucket bucket = new TokenBucket(10, 600); // 분당 600개 = 초당 10개
        
        // When
        Thread.sleep(2000); // 2초 대기 (20개 토큰이 리필될 수 있지만 용량은 10개)
        bucket.refill();
        
        // Then
        assertThat(bucket.getAvailableTokens()).isEqualTo(10); // 용량을 초과하지 않음
        assertThat(bucket.isFull()).isTrue();
    }
    
    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 토큰 소비")
    void concurrency_MultipleThreadsConsumeTokens() throws Exception {
        // Given
        TokenBucket bucket = new TokenBucket(100, 60);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When
        Future<?>[] futures = new Future[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = executor.submit(() -> {
                if (bucket.tryConsume(10)) {
                    successCount.incrementAndGet();
                }
            });
        }
        
        // 모든 작업 완료 대기
        for (Future<?> future : futures) {
            future.get();
        }
        
        // Then
        assertThat(successCount.get()).isEqualTo(10); // 100개 토큰으로 10개씩 10번 소비 가능
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("동시성 테스트 - 토큰 경쟁 상황")
    void concurrency_TokenCompetition() throws Exception {
        // Given
        TokenBucket bucket = new TokenBucket(50, 60);
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        // When
        Future<?>[] futures = new Future[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = executor.submit(() -> {
                if (bucket.tryConsume(5)) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            });
        }
        
        // 모든 작업 완료 대기
        for (Future<?> future : futures) {
            future.get();
        }
        
        // Then
        assertThat(successCount.get()).isEqualTo(10); // 50개 토큰으로 5개씩 10번만 성공 가능
        assertThat(failCount.get()).isEqualTo(10); // 나머지 10번은 실패
        assertThat(bucket.getAvailableTokens()).isEqualTo(0); // 모든 토큰 소비됨
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("리필 시간 업데이트 테스트")
    void refill_UpdatesLastRefillTime() throws InterruptedException {
        // Given
        TokenBucket bucket = new TokenBucket(60, 60);
        LocalDateTime initialRefillTime = bucket.getLastRefillTime();
        bucket.tryConsume(10); // 토큰 일부 소비
        
        // When
        Thread.sleep(1100); // 1초 이상 대기
        bucket.refill();
        
        // Then
        assertThat(bucket.getLastRefillTime()).isAfter(initialRefillTime);
    }
}