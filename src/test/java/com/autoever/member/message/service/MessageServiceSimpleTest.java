package com.autoever.member.message.service;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.MessageApiClient;
import com.autoever.member.message.client.MessageClientFactory;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MessageService 단순 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Simple Test")
class MessageServiceSimpleTest {
    
    @Mock
    private MessageClientFactory clientFactory;
    
    @Mock
    private MessageMonitoringService monitoringService;
    
    @Mock
    private MessageApiClient client;
    
    private MessageService messageService;
    private MessageRequest messageRequest;
    
    @BeforeEach
    void setUp() {
        messageService = new MessageService(clientFactory, monitoringService);
        messageRequest = new MessageRequest("010-1234-5678", "테스트 메시지");
    }
    
    @Test
    @DisplayName("메시지 발송 성공")
    void sendMessage_Success() {
        // Given
        RetryPolicy retryPolicy = RetryPolicy.noRetryPolicy();
        MessageResponse successResponse = MessageResponse.success("msg-123", ApiType.KAKAOTALK);
        
        when(client.getApiType()).thenReturn(ApiType.KAKAOTALK);
        when(clientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(client);
        when(client.sendMessage(messageRequest)).thenReturn(successResponse);
        
        // When
        MessageResponse response = messageService.sendMessage(messageRequest, ApiType.KAKAOTALK, retryPolicy);
        
        // Then
        assertThat(response).isEqualTo(successResponse);
        assertThat(response.success()).isTrue();
        
        verify(clientFactory).getAvailableClient(ApiType.KAKAOTALK);
        verify(client).sendMessage(messageRequest);
        verify(monitoringService).recordMessageAttempt(any());
    }
    
    @Test
    @DisplayName("클라이언트 없음")
    void sendMessage_NoClient() {
        // Given
        RetryPolicy retryPolicy = RetryPolicy.noRetryPolicy();
        
        when(clientFactory.getAvailableClient(ApiType.KAKAOTALK)).thenReturn(null);
        when(clientFactory.getAvailableClientWithTimeout(ApiType.KAKAOTALK, 5000)).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> messageService.sendMessage(messageRequest, ApiType.KAKAOTALK, retryPolicy))
            .isInstanceOf(RuntimeException.class);
        
        verify(clientFactory).getAvailableClient(ApiType.KAKAOTALK);
        verify(clientFactory).getAvailableClientWithTimeout(ApiType.KAKAOTALK, 5000);
    }
    
    @Test
    @DisplayName("건강성 체크 - 정상")
    void isHealthy_True() {
        // Given
        when(clientFactory.getHealthyClient()).thenReturn(client);
        when(client.isAvailable()).thenReturn(true);
        
        // When
        boolean healthy = messageService.isMessageServiceHealthy();
        
        // Then
        assertThat(healthy).isTrue();
        verify(clientFactory).getHealthyClient();
        verify(client).isAvailable();
    }
    
    @Test
    @DisplayName("건강성 체크 - 비정상")
    void isHealthy_False() {
        // Given
        when(clientFactory.getHealthyClient()).thenReturn(null);
        
        // When
        boolean healthy = messageService.isMessageServiceHealthy();
        
        // Then
        assertThat(healthy).isFalse();
        verify(clientFactory).getHealthyClient();
    }
    
    @Test
    @DisplayName("상태 새로고침")
    void refresh() {
        // When
        messageService.refreshClientStatus();
        
        // Then
        verify(clientFactory).refresh();
    }
}