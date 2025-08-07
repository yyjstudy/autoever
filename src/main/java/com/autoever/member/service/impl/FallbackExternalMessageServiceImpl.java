package com.autoever.member.service.impl;

import com.autoever.member.message.result.MessageSendResult;
import com.autoever.member.message.service.FallbackMessageService;
import com.autoever.member.service.ExternalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FallbackMessageService를 사용하는 외부 메시지 발송 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackExternalMessageServiceImpl implements ExternalMessageService {
    
    private final FallbackMessageService fallbackMessageService;
    
    @Override
    public void sendMessage(String phoneNumber, String message) {
        try {
            MessageSendResult result = fallbackMessageService.sendWithFallback("사용자", phoneNumber, message);
            
            if (result == MessageSendResult.SUCCESS_KAKAO || result == MessageSendResult.SUCCESS_SMS_FALLBACK) {
                log.debug("메시지 발송 성공 - 수신자: {}, 결과: {}", phoneNumber, result);
            } else {
                throw new RuntimeException("메시지 발송 실패: " + result);
            }
            
        } catch (Exception e) {
            log.error("메시지 발송 중 오류 발생 - 수신자: {}, 메시지: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("메시지 발송 실패: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        // KakaoTalk이나 SMS 중 하나라도 사용 가능하면 true
        return true; // FallbackMessageService는 내부적으로 클라이언트 상태를 확인함
    }
}