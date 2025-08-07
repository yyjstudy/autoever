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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SmsApiClient 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmsApiClient 테스트")
class SmsApiClientTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    
    private SmsApiClient smsApiClient;
    private MessageApiConfig messageApiConfig;
    
    @BeforeEach
    void setUp() {
        // MessageApiConfig 설정
        messageApiConfig = new MessageApiConfig();
        MessageApiConfig.SmsConfig smsConfig = messageApiConfig.getSms();
        smsConfig.setBaseUrl("http://localhost:8082");
        smsConfig.setUsername("autoever");
        smsConfig.setPassword("5678");
        
        // RestTemplateBuilder mock 설정
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        smsApiClient = new SmsApiClient(messageApiConfig, restTemplateBuilder);
    }
    
    @Test
    @DisplayName("SMS 메시지 발송 성공 테스트")
    void sendMessage_Success() {
        // Given
        MessageRequest request = new MessageRequest("010-9876-5432", "SMS 테스트 메시지");
        Map<String, Object> responseBody = Map.of("smsId", "sms_msg_456");
        ResponseEntity<Map> mockResponse = ResponseEntity.ok(responseBody);
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(mockResponse);
        
        // When
        MessageResponse response = smsApiClient.sendMessage(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.messageId()).isEqualTo("sms_msg_456");
        assertThat(response.apiType()).isEqualTo(ApiType.SMS);
        assertThat(response.errorCode()).isNull();
        assertThat(response.errorMessage()).isNull();
        assertThat(response.timestamp()).isNotNull();
        
        verify(restTemplate).postForEntity(
            eq("http://localhost:8082/sms?phone=010-9876-5432"),
            any(),
            eq(Map.class)
        );
    }
    
    @Test
    @DisplayName("클라이언트 오류 시 실패 응답 반환 테스트")
    void sendMessage_ClientError_ReturnsFailureResponse() {
        // Given
        MessageRequest request = new MessageRequest("010-9876-5432", "SMS 테스트");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));
        
        // When
        MessageResponse response = smsApiClient.sendMessage(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.success()).isFalse();
        assertThat(response.messageId()).isNull();
        assertThat(response.apiType()).isEqualTo(ApiType.SMS);
        assertThat(response.errorCode()).isEqualTo("CLIENT_ERROR");
        assertThat(response.errorMessage()).contains("클라이언트 오류: 400 BAD_REQUEST");
    }
    
    @Test
    @DisplayName("API 연결 실패 시 예외 발생 테스트")
    void sendMessage_ConnectionFailed_ThrowsException() {
        // Given
        MessageRequest request = new MessageRequest("010-9876-5432", "SMS 테스트");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));
        
        // When & Then
        assertThatThrownBy(() -> smsApiClient.sendMessage(request))
            .isInstanceOf(ApiConnectionException.class)
            .hasMessageContaining("SMS API 서버에 연결할 수 없습니다");
    }
    
    @Test
    @DisplayName("API 타입 반환 테스트")
    void getApiType_ReturnsSms() {
        // When
        ApiType apiType = smsApiClient.getApiType();
        
        // Then
        assertThat(apiType).isEqualTo(ApiType.SMS);
    }
    
    @Test
    @DisplayName("연결 상태 확인 성공 테스트")
    void validateConnection_Success() {
        // Given
        ResponseEntity<String> healthResponse = ResponseEntity.ok("healthy");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(healthResponse);
        
        // When
        boolean isAvailable = smsApiClient.validateConnection();
        
        // Then
        assertThat(isAvailable).isTrue();
        verify(restTemplate).exchange(
            eq("http://localhost:8082/health"),
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
            .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        
        // When
        boolean isAvailable = smsApiClient.validateConnection();
        
        // Then
        assertThat(isAvailable).isFalse();
    }
    
    @Test
    @DisplayName("isAvailable은 validateConnection 결과를 반환")
    void isAvailable_ReturnsValidateConnectionResult() {
        // Given
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Network error"));
        
        // When
        boolean isAvailable = smsApiClient.isAvailable();
        
        // Then
        assertThat(isAvailable).isFalse();
    }
}