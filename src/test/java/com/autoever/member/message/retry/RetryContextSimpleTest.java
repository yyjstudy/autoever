package com.autoever.member.message.retry;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.exception.MessageApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * RetryContext 단순 테스트
 */
@DisplayName("RetryContext Simple Test")
class RetryContextSimpleTest {
    
    private MessageRequest messageRequest;
    private RetryPolicy retryPolicy;
    private RetryContext retryContext;
    
    @BeforeEach
    void setUp() {
        messageRequest = new MessageRequest("010-1234-5678", "테스트 메시지");
        retryPolicy = RetryPolicy.defaultPolicy(); // maxAttempts = 3
        retryContext = new RetryContext(messageRequest, retryPolicy);
    }
    
    @Test
    @DisplayName("초기 상태")
    void initialization() {
        assertThat(retryContext.getCurrentAttempt()).isEqualTo(0);
        assertThat(retryContext.canRetry()).isTrue(); // 0번 시도 후 재시도 가능
        assertThat(retryContext.getAttempts()).isEmpty();
    }
    
    @Test
    @DisplayName("첫 번째 시도")
    void firstAttempt() {
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        
        assertThat(retryContext.getCurrentAttempt()).isEqualTo(1);
        assertThat(retryContext.canRetry()).isTrue(); // 1번 시도 후 재시도 가능
    }
    
    @Test
    @DisplayName("성공 기록")
    void recordSuccess() {
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        retryContext.recordSuccess("성공");
        
        assertThat(retryContext.getAttempts()).hasSize(1);
        assertThat(retryContext.getAttempts().get(0).isSuccess()).isTrue();
    }
    
    @Test
    @DisplayName("실패 기록")
    void recordFailure() {
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        retryContext.recordFailure(new MessageApiException(ApiType.KAKAOTALK, "ERROR", "실패"), "상세");
        
        assertThat(retryContext.getAttempts()).hasSize(1);
        assertThat(retryContext.getAttempts().get(0).isSuccess()).isFalse();
    }
    
    @Test
    @DisplayName("최대 시도 도달")
    void maxAttemptsReached() {
        // 3번 시도 (maxAttempts = 3)
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        retryContext.startNewAttempt(ApiType.SMS);
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        
        assertThat(retryContext.getCurrentAttempt()).isEqualTo(3);
        assertThat(retryContext.canRetry()).isFalse(); // 3번 시도 후 재시도 불가
    }
    
    @Test
    @DisplayName("요약 생성")
    void createSummary() {
        retryContext.startNewAttempt(ApiType.KAKAOTALK);
        retryContext.recordSuccess("성공");
        
        RetryContext.RetrySummary summary = retryContext.createSummary();
        
        assertThat(summary.getTotalAttempts()).isEqualTo(1);
        assertThat(summary.isFinalSuccess()).isTrue();
        assertThat(summary.getAttempts()).hasSize(1);
    }
}