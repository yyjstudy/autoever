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
import com.autoever.member.message.queue.MessageQueueService;
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
    
    @Mock
    private MessageQueueService messageQueueService;
    
    private FallbackMessageService fallbackMessageService;
    
    @BeforeEach
    void setUp() {
        // Rate Limiter 기본 설정 - 허용 상태로 설정 (lenient로 설정)
        lenient().when(apiRateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(true);
        lenient().when(apiRateLimiter.tryAcquire(ApiType.SMS)).thenReturn(true);
        
        fallbackMessageService = new FallbackMessageService(
            kakaoTalkApiClient, smsApiClient, messageTemplateService, messageSendTracker, apiRateLimiter, messageQueueService);
    }
    
    @Test
    @DisplayName("큐에 메시지 추가 성공")
    void sendWithFallback_QueueSuccess() {
        // Given
        String memberName = "김철수";
        String phoneNumber = "010-1234-5678";
        String originalMessage = "테스트 메시지";
        String templatedMessage = "김철수님, 안녕하세요. 테스트 메시지";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(messageQueueService.enqueue(eq(memberName), eq(phoneNumber), eq(templatedMessage), eq(ApiType.KAKAOTALK)))
            .thenReturn(MessageQueueService.QueueResult.queued("queue_id_123", 1));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.QUEUED);
        verify(messageSendTracker).recordResult(MessageSendResult.QUEUED, ApiType.KAKAOTALK);
        verify(messageQueueService).enqueue(memberName, phoneNumber, templatedMessage, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("큐에 메시지 추가 성공 (User 객체 사용)")
    void sendWithFallback_UserObject_QueueSuccess() {
        // Given
        User user = User.builder()
            .name("이영희")
            .phoneNumber("010-9876-5432")
            .build();
        String originalMessage = "중요한 안내사항";
        String templatedMessage = "이영희님, 안녕하세요. 중요한 안내사항";
        
        when(messageTemplateService.applyTemplate(user, originalMessage)).thenReturn(templatedMessage);
        when(messageQueueService.enqueue(eq("이영희"), eq("010-9876-5432"), eq(templatedMessage), eq(ApiType.KAKAOTALK)))
            .thenReturn(MessageQueueService.QueueResult.queued("queue_id_456", 2));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(user, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.QUEUED);
        verify(messageSendTracker).recordResult(MessageSendResult.QUEUED, ApiType.KAKAOTALK);
        verify(messageQueueService).enqueue("이영희", "010-9876-5432", templatedMessage, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("큐가 가득 찬 경우")
    void sendWithFallback_QueueFull() {
        // Given
        String memberName = "박민수";
        String phoneNumber = "010-5555-5555";
        String originalMessage = "실패 테스트";
        String templatedMessage = "박민수님, 안녕하세요. 실패 테스트";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(messageQueueService.enqueue(eq(memberName), eq(phoneNumber), eq(templatedMessage), eq(ApiType.KAKAOTALK)))
            .thenReturn(MessageQueueService.QueueResult.queueFull());
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.QUEUE_FULL);
        verify(messageSendTracker).recordResult(MessageSendResult.QUEUE_FULL, ApiType.KAKAOTALK);
        verify(messageQueueService).enqueue(memberName, phoneNumber, templatedMessage, ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("템플릿 적용 후 큐에 추가")
    void sendWithFallback_TemplateAndQueue() {
        // Given
        String memberName = "최영수";
        String phoneNumber = "010-7777-7777";
        String originalMessage = "큐 처리 테스트";
        String templatedMessage = "최영수님, 안녕하세요. 큐 처리 테스트";
        
        when(messageTemplateService.applyTemplate(memberName, originalMessage)).thenReturn(templatedMessage);
        when(messageQueueService.enqueue(eq(memberName), eq(phoneNumber), eq(templatedMessage), eq(ApiType.KAKAOTALK)))
            .thenReturn(MessageQueueService.QueueResult.queued("queue_id_789", 10));
        
        // When
        MessageSendResult result = fallbackMessageService.sendWithFallback(memberName, phoneNumber, originalMessage);
        
        // Then
        assertThat(result).isEqualTo(MessageSendResult.QUEUED);
        verify(messageTemplateService).applyTemplate(memberName, originalMessage);
        verify(messageQueueService).enqueue(memberName, phoneNumber, templatedMessage, ApiType.KAKAOTALK);
        verify(messageSendTracker).recordResult(MessageSendResult.QUEUED, ApiType.KAKAOTALK);
    }
}