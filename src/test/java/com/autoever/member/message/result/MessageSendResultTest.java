package com.autoever.member.message.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MessageSendResult 테스트")
class MessageSendResultTest {
    
    @Test
    @DisplayName("성공 상태 확인 - 카카오톡 성공")
    void isSuccess_KakaoSuccess_ReturnsTrue() {
        // when
        boolean result = MessageSendResult.SUCCESS_KAKAO.isSuccess();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("성공 상태 확인 - SMS Fallback 성공")
    void isSuccess_SmsFallback_ReturnsTrue() {
        // when
        boolean result = MessageSendResult.SUCCESS_SMS_FALLBACK.isSuccess();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("성공 상태 확인 - 모두 실패")
    void isSuccess_FailedBoth_ReturnsFalse() {
        // when
        boolean result = MessageSendResult.FAILED_BOTH.isSuccess();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("성공 상태 확인 - Rate Limited")
    void isSuccess_RateLimited_ReturnsFalse() {
        // when
        boolean result = MessageSendResult.RATE_LIMITED.isSuccess();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("Fallback 여부 확인 - SMS Fallback")
    void isFallback_SmsFallback_ReturnsTrue() {
        // when
        boolean result = MessageSendResult.SUCCESS_SMS_FALLBACK.isFallback();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("Fallback 여부 확인 - 카카오톡 성공")
    void isFallback_KakaoSuccess_ReturnsFalse() {
        // when
        boolean result = MessageSendResult.SUCCESS_KAKAO.isFallback();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("설명 확인")
    void getDescription() {
        // then
        assertThat(MessageSendResult.SUCCESS_KAKAO.getDescription()).isEqualTo("카카오톡 발송 성공");
        assertThat(MessageSendResult.SUCCESS_SMS_FALLBACK.getDescription()).isEqualTo("SMS 대체 발송 성공");
        assertThat(MessageSendResult.FAILED_BOTH.getDescription()).isEqualTo("모든 발송 방법 실패");
        assertThat(MessageSendResult.RATE_LIMITED.getDescription()).isEqualTo("발송량 제한으로 대기 중");
        assertThat(MessageSendResult.INVALID_RECIPIENT.getDescription()).isEqualTo("잘못된 수신자 정보");
    }
}