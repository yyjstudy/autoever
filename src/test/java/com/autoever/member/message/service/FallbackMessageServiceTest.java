package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.MessageApiClient;
import com.autoever.member.message.client.MessageClientFactory;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.result.MessageSendResult;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.template.MessageTemplateService;
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
@DisplayName("FallbackMessageService 테스트")
class FallbackMessageServiceTest {

    @Mock
    private MessageClientFactory messageClientFactory;
    
    @Mock
    private MessageTemplateService messageTemplateService;
    
    @Mock
    private MessageSendTracker messageSendTracker;
    
    @Mock
    private MessageApiClient kakaoTalkClient;
    
    @Mock
    private MessageApiClient smsClient;
    
    private FallbackMessageService fallbackMessageService;
    
    @BeforeEach
    void setUp() {
        fallbackMessageService = new FallbackMessageService(
            messageClientFactory, messageTemplateService, messageSendTracker);
    }
    
    @Test
    @DisplayName("User 객체로 카카오톡 발송 성공")
    void sendWithFallback_User_KakaoTalkSuccess() {
        // given
        User user = createTestUser("김철수", "010-1234-5678");
        String originalMessage = "테스트 메시지입니다.";
        String templatedMessage = "김철수님, 안녕하세요. 현대 오토에버입니다.\n\n테스트 메시지입니다.";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(kakaoTalkClient);
        when(messageTemplateService.applyTemplate(user, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("kakao_msg_123", ApiType.KAKAOTALK));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(user, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_KAKAO);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("카카오톡 실패 시 SMS Fallback 성공")
    void sendWithFallback_KakaoFailure_SmsFallbackSuccess() {
        // given
        String memberName = "이영희";
        String phoneNumber = "010-9876-5432";
        String originalMessage = "중요한 안내사항입니다.";
        String templatedMessage = "이영희님, 안녕하세요. 현대 오토에버입니다.\n\n중요한 안내사항입니다.";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(kakaoTalkClient);
        when(messageClientFactory.getAvailableClient(ApiType.SMS)).thenReturn(smsClient);
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("SERVER_ERROR", "서버 오류", ApiType.KAKAOTALK));
        when(smsClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("sms_msg_456", ApiType.SMS));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_SMS_FALLBACK);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
    }
    
    @Test
    @DisplayName("카카오톡과 SMS 모두 실패")
    void sendWithFallback_BothFailure() {
        // given
        User user = createTestUser("박민수", "010-5555-6666");
        String originalMessage = "테스트 메시지";
        String templatedMessage = "박민수님, 안녕하세요. 현대 오토에버입니다.\n\n테스트 메시지";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(kakaoTalkClient);
        when(messageClientFactory.getAvailableClient(ApiType.SMS)).thenReturn(smsClient);
        when(messageTemplateService.applyTemplate(user, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("NETWORK_ERROR", "네트워크 오류", ApiType.KAKAOTALK));
        when(smsClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("QUOTA_EXCEEDED", "발송 한도 초과", ApiType.SMS));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(user, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.FAILED_BOTH);
        verify(messageSendTracker).recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("카카오톡 클라이언트가 없는 경우 SMS로 바로 전환")
    void sendWithFallback_NoKakaoTalkClient_DirectSms() {
        // given
        String memberName = "최영희";
        String phoneNumber = "010-7777-8888";
        String originalMessage = "알림 메시지";
        String templatedMessage = "최영희님, 안녕하세요. 현대 오토에버입니다.\n\n알림 메시지";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(null);
        when(messageClientFactory.getAvailableClient(ApiType.SMS)).thenReturn(smsClient);
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(smsClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("sms_msg_789", ApiType.SMS));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_SMS_FALLBACK);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
    }
    
    @Test
    @DisplayName("SMS 클라이언트가 없는 경우 실패")
    void sendWithFallback_NoSmsClient_Failure() {
        // given
        User user = createTestUser("정민수", "010-9999-0000");
        String originalMessage = "테스트";
        String templatedMessage = "정민수님, 안녕하세요. 현대 오토에버입니다.\n\n테스트";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(kakaoTalkClient);
        when(messageClientFactory.getAvailableClient(ApiType.SMS)).thenReturn(null);
        when(messageTemplateService.applyTemplate(user, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.failure("TIMEOUT", "타임아웃", ApiType.KAKAOTALK));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(user, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.FAILED_BOTH);
        verify(messageSendTracker).recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("카카오톡 발송 중 예외 발생 시 SMS로 전환")
    void sendWithFallback_KakaoTalkException_SmsFallback() {
        // given
        String memberName = "김예외";
        String phoneNumber = "010-1111-2222";
        String originalMessage = "예외 테스트";
        String templatedMessage = "김예외님, 안녕하세요. 현대 오토에버입니다.\n\n예외 테스트";
        
        when(messageClientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(kakaoTalkClient);
        when(messageClientFactory.getAvailableClient(ApiType.SMS)).thenReturn(smsClient);
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(kakaoTalkClient.sendMessage(any(MessageRequest.class)))
            .thenThrow(new RuntimeException("네트워크 연결 실패"));
        when(smsClient.sendMessage(any(MessageRequest.class)))
            .thenReturn(MessageResponse.success("sms_msg_exception", ApiType.SMS));
        
        // when
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // then
        assertThat(result).isEqualTo(MessageSendResult.SUCCESS_SMS_FALLBACK);
        verify(messageSendTracker).recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
    }
    
    @Test
    @DisplayName("null User로 발송 시 예외 발생")
    void sendWithFallback_NullUser_ThrowsException() {
        // given
        String originalMessage = "테스트 메시지";
        
        // when & then
        assertThatThrownBy(() -> fallbackMessageService.sendWithFallback((User) null, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자 정보는 필수입니다");
    }
    
    @Test
    @DisplayName("null 회원 이름으로 발송 시 예외 발생")
    void sendWithFallback_NullMemberName_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String originalMessage = "테스트 메시지";
        
        // when & then
        assertThatThrownBy(() -> fallbackMessageService.sendWithFallback(null, phoneNumber, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 이름은 필수입니다");
    }
    
    @Test
    @DisplayName("빈 전화번호로 발송 시 예외 발생")
    void sendWithFallback_EmptyPhoneNumber_ThrowsException() {
        // given
        String memberName = "김철수";
        String phoneNumber = "   ";
        String originalMessage = "테스트 메시지";
        
        // when & then
        assertThatThrownBy(() -> fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("전화번호는 필수입니다");
    }
    
    @Test
    @DisplayName("null 메시지로 발송 시 예외 발생")
    void sendWithFallback_NullMessage_ThrowsException() {
        // given
        User user = createTestUser("김철수", "010-1234-5678");
        
        // when & then
        assertThatThrownBy(() -> fallbackMessageService.sendWithFallback(user, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("메시지 내용은 필수입니다");
    }
    
    @Test
    @DisplayName("빈 메시지로 발송 시 예외 발생")
    void sendWithFallback_EmptyMessage_ThrowsException() {
        // given
        String memberName = "김철수";
        String phoneNumber = "010-1234-5678";
        String originalMessage = "   ";
        
        // when & then
        assertThatThrownBy(() -> fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("메시지 내용은 필수입니다");
    }
    
    private User createTestUser(String name, String phoneNumber) {
        return User.builder()
            .username("testuser")
            .name(name)
            .phoneNumber(phoneNumber)
            .email("test@example.com")
            .socialNumber("900101-1234567")
            .address("서울시 강남구")
            .build();
    }
}