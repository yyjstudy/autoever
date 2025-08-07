package com.autoever.mock.sms.controller;

import com.autoever.mock.sms.dto.SmsRequest;
import com.autoever.mock.sms.dto.SmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * SMS Mock API 컨트롤러
 * Basic Auth 인증 후 POST /sms?phone={phone} 엔드포인트 제공
 * JSON 요청 바디를 처리합니다.
 */
@Slf4j
@RestController
public class SmsController {

    @PostMapping("/sms")
    public ResponseEntity<SmsResponse> sendSms(
            @RequestParam("phone") String phoneNumber,
            @RequestBody SmsRequest request) {
        
        log.info("SMS 요청 수신 - phone: {}, message: {}", 
                 maskPhoneNumber(phoneNumber), request.message());
        
        try {
            // 입력 검증
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("전화번호가 누락됨");
                return ResponseEntity.badRequest()
                    .body(SmsResponse.failure("INVALID_PHONE", "전화번호는 필수입니다"));
            }
            
            if (request.message() == null || request.message().trim().isEmpty()) {
                log.warn("메시지 내용이 누락됨");
                return ResponseEntity.badRequest()
                    .body(SmsResponse.failure("INVALID_MESSAGE", "메시지 내용은 필수입니다"));
            }
            
            // 전화번호 형식 간단 검증
            if (!phoneNumber.matches("^\\d{3}-\\d{4}-\\d{4}$") && 
                !phoneNumber.matches("^\\d{11}$") && 
                !phoneNumber.matches("^010\\d{8}$")) {
                log.warn("잘못된 전화번호 형식 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.badRequest()
                    .body(SmsResponse.failure("INVALID_PHONE_FORMAT", "올바른 전화번호 형식이 아닙니다"));
            }
            
            // 시뮬레이션 시나리오 처리
            if (phoneNumber.contains("9999")) {
                log.warn("SMS 발송량 초과 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(SmsResponse.failure("QUOTA_EXCEEDED", "일일 발송량을 초과했습니다"));
            }
            
            if (phoneNumber.contains("8888")) {
                log.warn("SMS 서버 오류 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SmsResponse.failure("SERVER_ERROR", "SMS 서버에 오류가 발생했습니다"));
            }
            
            if (phoneNumber.contains("7777")) {
                log.warn("SMS 네트워크 오류 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(SmsResponse.failure("NETWORK_ERROR", "SMS 게이트웨이 연결에 실패했습니다"));
            }
            
            if (phoneNumber.contains("6666")) {
                log.warn("잘못된 번호 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.badRequest()
                    .body(SmsResponse.failure("INVALID_RECIPIENT", "유효하지 않은 수신번호입니다"));
            }
            
            // 정상 처리
            String messageId = "sms_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("SMS 발송 성공 - messageId: {}, phone: {}", 
                     messageId, maskPhoneNumber(phoneNumber));
            
            return ResponseEntity.ok(SmsResponse.success(messageId));
            
        } catch (Exception e) {
            log.error("SMS 처리 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SmsResponse.failure("UNEXPECTED_ERROR", "예기치 않은 오류가 발생했습니다"));
        }
    }
    
    /**
     * 전화번호를 마스킹합니다.
     */
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