package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.KakaoTalkApiClient;
import com.autoever.member.message.client.SmsApiClient;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.result.MessageSendResult;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.template.MessageTemplateService;
import com.autoever.member.message.ratelimit.ApiRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FallbackMessageService 간단 테스트")
class FallbackMessageServiceSimpleTest {

    @Mock
    private KakaoTalkApiClient kakaoTalkApiClient;
    
    @Mock
    private SmsApiClient smsApiClient;
    
    @Mock
    private MessageTemplateService messageTemplateService;
    
    @Mock
    private MessageSendTracker messageSendTracker;
    
    @Mock
    private ApiRateLimiter apiRateLimiter;
    
    private FallbackMessageService fallbackMessageService;
    
    @BeforeEach
    void setUp() {
        // Rate Limiter 기본 설정 - 허용 상태로 설정 (lenient로 설정)
        lenient().when(apiRateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(true);
        lenient().when(apiRateLimiter.tryAcquire(ApiType.SMS)).thenReturn(true);
        
        fallbackMessageService = new FallbackMessageService(
            kakaoTalkApiClient, smsApiClient, messageTemplateService, messageSendTracker, apiRateLimiter);
    }
    
    @Test
    @DisplayName("카카오톡 발송 성공")
    void sendWithFallback_KakaoTalkSuccess() {
        // Given
        String memberName = "김철수";
        String phoneNumber = "010-1234-5678";
        String originalMessage = "테스트 메시지";
        String templatedMessage = "김철수님, 안녕하세요. 테스트 메시지";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkApiClient.isAvailable()).thenReturn(true);
        when(kakaoTalkApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("kakao_msg_123", ApiType.KAKAOTALK));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_KAKAO);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("카카오톡 실패 시 SMS Fallback 성공")
    void sendWithFallback_KakaoFailure_SmsFallbackSuccess() {
        // Given
        String memberName = "이영희";
        String phoneNumber = "010-9876-5432";
        String originalMessage = "중요한 안내사항";
        String templatedMessage = "이영희님, 안녕하세요. 중요한 안내사항";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkApiClient.isAvailable()).thenReturn(true);
        when(kakaoTalkApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("SEND_ERROR", "전송 실패", ApiType.KAKAOTALK));
        when(smsApiClient.isAvailable()).thenReturn(true);
        when(smsApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("sms_msg_456", ApiType.SMS));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_SMS_FALLBACK);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
    }
    
    @Test
    @DisplayName("모든 발송 방법 실패")
    void sendWithFallback_AllFailure() {
        // Given
        String memberName = "박민수";
        String phoneNumber = "010-5555-5555";
        String originalMessage = "실패 테스트";
        String templatedMessage = "박민수님, 안녕하세요. 실패 테스트";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkApiClient.isAvailable()).thenReturn(true);
        when(kakaoTalkApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("SEND_ERROR", "카카오톡 전송 실패", ApiType.KAKAOTALK));
        when(smsApiClient.isAvailable()).thenReturn(true);
        when(smsApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("SEND_ERROR", "SMS 전송 실패", ApiType.SMS));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.FAILED_BOTH);
        verify(messageSendTracker).recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("Rate Limiting 처리")
    void sendWithFallback_RateLimited() {
        // Given
        String memberName = "최영수";
        String phoneNumber = "010-7777-7777";
        String originalMessage = "Rate Limit 테스트";
        String templatedMessage = "최영수님, 안녕하세요. Rate Limit 테스트";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkApiClient.isAvailable()).thenReturn(true);
        when(kakaoTalkApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("RATE_LIMIT_EXCEEDED", "Rate Limit 초과", ApiType.KAKAOTALK));
        when(smsApiClient.isAvailable()).thenReturn(true);
        when(smsApiClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("RATE_LIMIT_EXCEEDED", "SMS Rate Limit 초과", ApiType.SMS));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.RATE_LIMITED);
        verify(messageSendTracker).recordResult(MessageSendResult.RATE_LIMITED, ApiType.SMS);
    }
}