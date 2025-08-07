package com.autoever.mock.kakaotalk.controller;

import com.autoever.mock.kakaotalk.dto.KakaoTalkMessageRequest;
import com.autoever.mock.kakaotalk.dto.KakaoTalkMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * KakaoTalk Mock API 컨트롤러
 * Basic Auth 인증 후 POST /kakaotalk-messages 엔드포인트 제공
 */
@Slf4j
@RestController
public class KakaoTalkController {

    @PostMapping("/kakaotalk-messages")
    public ResponseEntity<Void> sendMessage(@RequestBody KakaoTalkMessageRequest request) {
        log.info("KakaoTalk 메시지 요청 수신 - phone: {}, message: {}", 
                 maskPhoneNumber(request.getPhoneNumber()), request.getMessage());
        
        try {
            // 입력 검증
            if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
                log.warn("전화번호가 누락됨");
                return ResponseEntity.badRequest().build();
            }
            
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                log.warn("메시지 내용이 누락됨");
                return ResponseEntity.badRequest().build();
            }
            
            // 시뮬레이션 시나리오 처리
            String phoneNumber = request.getPhoneNumber();
            
            // 특정 번호로 에러 시나리오 테스트
            if (phoneNumber.contains("9999")) {
                log.warn("서버 오류 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            if (phoneNumber.contains("8888")) {
                log.warn("네트워크 오류 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            
            if (phoneNumber.contains("7777")) {
                log.warn("타임아웃 시뮬레이션 - phone: {}", maskPhoneNumber(phoneNumber));
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
            
            // 정상 처리
            String messageId = "kakao_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("KakaoTalk 메시지 발송 성공 - messageId: {}, phone: {}", 
                     messageId, maskPhoneNumber(phoneNumber));
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("KakaoTalk 메시지 처리 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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