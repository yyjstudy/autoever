package com.autoever.member.message.client;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.config.MessageApiConfig;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.exception.ApiConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KakaoTalkApiClient 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoTalkApiClient 테스트")
class KakaoTalkApiClientTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    
    private KakaoTalkApiClient kakaoTalkApiClient;
    private MessageApiConfig messageApiConfig;
    
    @BeforeEach
    void setUp() {
        // MessageApiConfig 설정
        messageApiConfig = new MessageApiConfig();
        MessageApiConfig.KakaoTalkConfig kakaoConfig = messageApiConfig.getKakaotalk();
        kakaoConfig.setBaseUrl("http://localhost:8081");
        kakaoConfig.setUsername("autoever");
        kakaoConfig.setPassword("1234");
        
        // RestTemplateBuilder mock 설정
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        kakaoTalkApiClient = new KakaoTalkApiClient(messageApiConfig, restTemplateBuilder);
    }
    
    @Test
    @DisplayName("메시지 발송 성공 테스트")
    void sendMessage_Success() {
        // Given
        MessageRequest request = new MessageRequest("010-1234-5678", "테스트 메시지");
        Map<String, Object> responseBody = Map.of("messageId", "kakao_msg_123");
        ResponseEntity<Map> mockResponse = ResponseEntity.ok(responseBody);
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(mockResponse);
        
        // When
        MessageResponse response = kakaoTalkApiClient.sendMessage(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.messageId()).isEqualTo("kakao_msg_123");
        assertThat(response.apiType()).isEqualTo(ApiType.KAKAOTALK);
        assertThat(response.errorCode()).isNull();
        assertThat(response.errorMessage()).isNull();
        assertThat(response.timestamp()).isNotNull();
        
        verify(restTemplate).postForEntity(
            eq("http://localhost:8081/kakaotalk-messages"),
            any(),
            eq(Map.class)
        );
    }
    
    @Test
    @DisplayName("API 연결 실패 시 예외 발생 테스트")
    void sendMessage_ConnectionFailed_ThrowsException() {
        // Given
        MessageRequest request = new MessageRequest("010-1234-5678", "테스트 메시지");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new ResourceAccessException("Connection timeout"));
        
        // When & Then
        assertThatThrownBy(() -> kakaoTalkApiClient.sendMessage(request))
            .isInstanceOf(ApiConnectionException.class)
            .hasMessageContaining("카카오톡 API 서버에 연결할 수 없습니다");
    }
    
    @Test
    @DisplayName("API 타입 반환 테스트")
    void getApiType_ReturnsKakaoTalk() {
        // When
        ApiType apiType = kakaoTalkApiClient.getApiType();
        
        // Then
        assertThat(apiType).isEqualTo(ApiType.KAKAOTALK);
    }
    
    @Test
    @DisplayName("연결 상태 확인 성공 테스트")
    void validateConnection_Success() {
        // Given
        ResponseEntity<String> healthResponse = ResponseEntity.ok("OK");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(healthResponse);
        
        // When
        boolean isAvailable = kakaoTalkApiClient.validateConnection();
        
        // Then
        assertThat(isAvailable).isTrue();
        verify(restTemplate).exchange(
            eq("http://localhost:8081/health"),
            any(),
            any(),
            eq(String.class)
        );
    }
    
    @Test
    @DisplayName("연결 상태 확인 실패 테스트")
    void validateConnection_Failed() {
        // Given
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection timeout"));
        
        // When
        boolean isAvailable = kakaoTalkApiClient.validateConnection();
        
        // Then
        assertThat(isAvailable).isFalse();
    }
    
    @Test
    @DisplayName("isAvailable은 validateConnection 결과를 반환")
    void isAvailable_ReturnsValidateConnectionResult() {
        // Given
        ResponseEntity<String> healthResponse = ResponseEntity.ok("OK");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(healthResponse);
        
        // When
        boolean isAvailable = kakaoTalkApiClient.isAvailable();
        
        // Then
        assertThat(isAvailable).isTrue();
    }
}