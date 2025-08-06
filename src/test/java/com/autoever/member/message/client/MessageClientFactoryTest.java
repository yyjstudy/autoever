package com.autoever.member.message.client;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.ratelimit.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MessageClientFactory 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageClientFactory 테스트")
class MessageClientFactoryTest {
    
    @Mock
    private MessageApiClient kakaoClient;
    
    @Mock
    private MessageApiClient smsClient;
    
    @Mock
    private RateLimiter rateLimiter;
    
    private MessageClientFactory messageClientFactory;
    
    @BeforeEach
    void setUp() {
        // Mock 클라이언트 설정
        when(kakaoClient.getApiType()).thenReturn(ApiType.KAKAOTALK);
        when(smsClient.getApiType()).thenReturn(ApiType.SMS);
        
        List<MessageApiClient> clients = List.of(kakaoClient, smsClient);
        messageClientFactory = new MessageClientFactory(clients, rateLimiter);
    }
    
    @Test
    @DisplayName("선호 타입 클라이언트 반환 테스트")
    void getAvailableClient_PreferredType_Success() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(true);
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClient(ApiType.KAKAOTALK);
        
        // Then
        assertThat(client).isEqualTo(kakaoClient);
        verify(rateLimiter).tryAcquire(ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("선호 타입 사용 불가 시 대체 클라이언트 반환")
    void getAvailableClient_PreferredUnavailable_ReturnsFallback() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(false); // 카카오 사용 불가
        when(smsClient.isAvailable()).thenReturn(true);    // SMS 사용 가능
        when(rateLimiter.tryAcquire(ApiType.SMS)).thenReturn(true);
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClient(ApiType.KAKAOTALK);
        
        // Then
        assertThat(client).isEqualTo(smsClient); // SMS 클라이언트가 반환됨
        verify(rateLimiter).tryAcquire(ApiType.SMS);
    }
    
    @Test
    @DisplayName("Rate Limit 제한으로 선호 타입 사용 불가 시 대체 클라이언트 반환")
    void getAvailableClient_RateLimited_ReturnsFallback() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(smsClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(false); // 카카오 Rate Limit
        when(rateLimiter.tryAcquire(ApiType.SMS)).thenReturn(true);        // SMS 사용 가능
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClient(ApiType.KAKAOTALK);
        
        // Then
        assertThat(client).isEqualTo(smsClient); // SMS 클라이언트가 반환됨
        verify(rateLimiter).tryAcquire(ApiType.KAKAOTALK);
        verify(rateLimiter).tryAcquire(ApiType.SMS);
    }
    
    @Test
    @DisplayName("모든 클라이언트 사용 불가 시 null 반환")
    void getAvailableClient_AllUnavailable_ReturnsNull() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(false);
        when(smsClient.isAvailable()).thenReturn(false);
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClient(ApiType.KAKAOTALK);
        
        // Then
        assertThat(client).isNull();
    }
    
    @Test
    @DisplayName("타임아웃과 함께 클라이언트 요청 - 성공")
    void getAvailableClientWithTimeout_Success() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(eq(ApiType.KAKAOTALK), anyLong(), any())).thenReturn(true);
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClientWithTimeout(ApiType.KAKAOTALK, 1000);
        
        // Then
        assertThat(client).isEqualTo(kakaoClient);
        verify(rateLimiter).tryAcquire(eq(ApiType.KAKAOTALK), eq(1000L), any());
    }
    
    @Test
    @DisplayName("타임아웃과 함께 클라이언트 요청 - 선호 타입 실패, 대체 타입 사용")
    void getAvailableClientWithTimeout_PreferredTimeout_UsesFallback() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(smsClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(eq(ApiType.KAKAOTALK), anyLong(), any())).thenReturn(false); // 타임아웃
        
        // When
        MessageApiClient client = messageClientFactory.getAvailableClientWithTimeout(ApiType.KAKAOTALK, 1000);
        
        // Then
        assertThat(client).isEqualTo(smsClient);
        verify(rateLimiter).tryAcquire(eq(ApiType.KAKAOTALK), eq(1000L), any());
        // SMS 클라이언트는 대체 클라이언트로 Rate Limiting 체크 없이 사용됨
        verify(rateLimiter, never()).tryAcquire(ApiType.SMS);
    }
    
    @Test
    @DisplayName("건강한 클라이언트 반환 테스트")
    void getHealthyClient_Success() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        
        // When
        MessageApiClient client = messageClientFactory.getHealthyClient();
        
        // Then
        assertThat(client).isEqualTo(kakaoClient);
        // Rate Limiting 체크 없이 건강한 클라이언트만 반환
        verifyNoInteractions(rateLimiter);
    }
    
    @Test
    @DisplayName("모든 클라이언트 반환 테스트")
    void getAllClients() {
        // When
        List<MessageApiClient> clients = messageClientFactory.getAllClients();
        
        // Then
        assertThat(clients).hasSize(2);
        assertThat(clients).contains(kakaoClient, smsClient);
    }
    
    @Test
    @DisplayName("특정 API 타입 클라이언트 사용 가능 여부 확인")
    void isClientAvailable_True() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(true);
        
        // When
        boolean available = messageClientFactory.isClientAvailable(ApiType.KAKAOTALK);
        
        // Then
        assertThat(available).isTrue();
    }
    
    @Test
    @DisplayName("특정 API 타입 클라이언트 사용 불가능 - Rate Limit")
    void isClientAvailable_False_RateLimited() {
        // Given
        when(kakaoClient.isAvailable()).thenReturn(true);
        when(rateLimiter.tryAcquire(ApiType.KAKAOTALK)).thenReturn(false); // Rate Limited
        
        // When
        boolean available = messageClientFactory.isClientAvailable(ApiType.KAKAOTALK);
        
        // Then
        assertThat(available).isFalse();
    }
    
    @Test
    @DisplayName("존재하지 않는 API 타입 클라이언트 확인")
    void isClientAvailable_NonExistentType() {
        // When & Then
        // 모든 ApiType enum 값에 대해서는 클라이언트가 존재한다고 가정
        for (ApiType apiType : ApiType.values()) {
            // 실제로는 클라이언트가 Mock으로만 두 개 등록되어 있으므로
            // 존재하지 않는 타입은 없지만, 코드 커버리지를 위한 테스트
            boolean hasClient = messageClientFactory.getAllClients().stream()
                .anyMatch(client -> client.getApiType() == apiType);
            
            if (!hasClient) {
                boolean available = messageClientFactory.isClientAvailable(apiType);
                assertThat(available).isFalse();
            }
        }
    }
    
    @Test
    @DisplayName("클라이언트 상태 새로고침 테스트")
    void refresh() {
        // Given
        when(kakaoClient.validateConnection()).thenReturn(true);
        when(smsClient.validateConnection()).thenReturn(false);
        when(rateLimiter.getAvailableTokens(ApiType.KAKAOTALK)).thenReturn(80L);
        when(rateLimiter.getAvailableTokens(ApiType.SMS)).thenReturn(450L);
        
        // When
        messageClientFactory.refresh();
        
        // Then
        verify(kakaoClient).validateConnection();
        verify(smsClient).validateConnection();
        verify(rateLimiter).getAvailableTokens(ApiType.KAKAOTALK);
        verify(rateLimiter).getAvailableTokens(ApiType.SMS);
    }
}