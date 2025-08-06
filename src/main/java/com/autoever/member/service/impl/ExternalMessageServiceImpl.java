package com.autoever.member.service.impl;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.service.MessageService;
import com.autoever.member.service.ExternalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 외부 메시지 발송 서비스 구현체
 * Task 9의 MessageService를 사용하여 실제 메시지 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalMessageServiceImpl implements ExternalMessageService {
    
    private final MessageService messageService;
    
    @Override
    public void sendMessage(String phoneNumber, String message) {
        try {
            MessageRequest request = new MessageRequest(phoneNumber, message);
            MessageResponse response = messageService.sendMessage(request, ApiType.KAKAOTALK);
            
            if (!response.success()) {
                throw new RuntimeException("메시지 발송 실패: " + response.errorMessage());
            }
            
            log.debug("메시지 발송 성공 - 수신자: {}, ID: {}", phoneNumber, response.messageId());
            
        } catch (Exception e) {
            log.error("메시지 발송 중 오류 발생 - 수신자: {}, 메시지: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("메시지 발송 실패: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return messageService.isMessageServiceHealthy();
    }
}