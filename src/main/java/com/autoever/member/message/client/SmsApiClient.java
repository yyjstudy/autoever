package com.autoever.member.message.client;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.config.MessageApiConfig;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.exception.ApiConnectionException;
import com.autoever.member.message.exception.MessageSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * SMS API 클라이언트 구현체
 */
@Component
public class SmsApiClient implements MessageApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(SmsApiClient.class);
    
    private final RestTemplate restTemplate;
    private final MessageApiConfig.SmsConfig config;
    private final String authHeader;
    
    public SmsApiClient(MessageApiConfig messageApiConfig, RestTemplateBuilder restTemplateBuilder) {
        this.config = messageApiConfig.getSms();
        this.authHeader = createBasicAuthHeader(config.getUsername(), config.getPassword());
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(config.getReadTimeoutMs()))
            .build();
    }
    
    @Override
    public MessageResponse sendMessage(MessageRequest request) {
        log.info("SMS 메시지 발송 시작: recipient={}", maskPhoneNumber(request.recipient()));
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authHeader);
            
            // SMS API 요청 형식에 맞게 변환
            Map<String, Object> requestBody = Map.of(
                "phoneNumber", request.recipient(),
                "content", request.message()
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = config.getBaseUrl() + "/sms";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("smsId");
                
                log.info("SMS 메시지 발송 성공: recipient={}, smsId={}", 
                    maskPhoneNumber(request.recipient()), messageId);
                
                return MessageResponse.success(messageId, ApiType.SMS);
            } else {
                log.warn("SMS 메시지 발송 실패: 예상하지 못한 응답 상태={}", response.getStatusCode());
                return MessageResponse.failure("UNEXPECTED_RESPONSE", 
                    "예상하지 못한 응답: " + response.getStatusCode(), ApiType.SMS);
            }
            
        } catch (HttpClientErrorException e) {
            log.warn("SMS 메시지 발송 클라이언트 오류: status={}, body={}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return MessageResponse.failure("CLIENT_ERROR", 
                "클라이언트 오류: " + e.getStatusCode(), ApiType.SMS);
                
        } catch (HttpServerErrorException e) {
            log.error("SMS 메시지 발송 서버 오류: status={}, body={}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return MessageResponse.failure("SERVER_ERROR", 
                "서버 오류: " + e.getStatusCode(), ApiType.SMS);
                
        } catch (ResourceAccessException e) {
            log.error("SMS API 연결 실패", e);
            throw new ApiConnectionException(ApiType.SMS, "SMS API 서버에 연결할 수 없습니다", e);
            
        } catch (Exception e) {
            log.error("SMS 메시지 발송 중 예상하지 못한 오류 발생", e);
            throw new MessageSendException(ApiType.SMS, "UNEXPECTED_ERROR", 
                "메시지 발송 중 예상하지 못한 오류가 발생했습니다", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return validateConnection();
    }
    
    @Override
    public ApiType getApiType() {
        return ApiType.SMS;
    }
    
    @Override
    public boolean validateConnection() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String healthCheckUrl = config.getBaseUrl() + "/health";
            
            ResponseEntity<String> response = restTemplate.exchange(
                healthCheckUrl, HttpMethod.GET, entity, String.class);
                
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.debug("SMS API 연결 상태 확인: healthy={}", isHealthy);
            
            return isHealthy;
            
        } catch (Exception e) {
            log.debug("SMS API 연결 확인 실패", e);
            return false;
        }
    }
    
    private String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        
        if (phoneNumber.contains("-") && phoneNumber.length() == 13) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        return "***-****-****";
    }
}