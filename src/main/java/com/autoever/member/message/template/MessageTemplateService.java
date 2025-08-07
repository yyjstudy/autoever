package com.autoever.member.message.template;

import com.autoever.member.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 메시지 템플릿 서비스
 * 메시지 발송 시 표준 템플릿을 적용하는 서비스입니다.
 */
@Slf4j
@Service
public class MessageTemplateService {
    
    /**
     * 사용자 정보를 기반으로 템플릿을 적용합니다.
     * 
     * @param user 사용자 엔티티
     * @param originalMessage 원본 메시지
     * @return 템플릿이 적용된 메시지
     */
    public String applyTemplate(User user, String originalMessage) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보는 필수입니다.");
        }
        
        String memberName = user.getName();
        String templatedMessage = MessageTemplate.applyTemplate(memberName, originalMessage);
        
        log.debug("메시지 템플릿 적용 완료 - 사용자: {}, 메시지 길이: {}", 
                user.getUsername(), templatedMessage.length());
        
        return templatedMessage;
    }
    
    /**
     * 회원 이름과 메시지 내용으로 템플릿을 적용합니다.
     * 
     * @param memberName 회원 이름
     * @param originalMessage 원본 메시지
     * @return 템플릿이 적용된 메시지
     */
    public String applyTemplate(String memberName, String originalMessage) {
        String templatedMessage = MessageTemplate.applyTemplate(memberName, originalMessage);
        
        log.debug("메시지 템플릿 적용 완료 - 이름: {}, 메시지 길이: {}", 
                memberName, templatedMessage.length());
        
        return templatedMessage;
    }
    
    /**
     * 템플릿이 올바르게 적용되었는지 검증합니다.
     * 
     * @param message 검증할 메시지
     * @param memberName 회원 이름
     * @return 템플릿이 올바르게 적용되었으면 true
     */
    public boolean isTemplateApplied(String message, String memberName) {
        if (message == null || memberName == null) {
            return false;
        }
        
        String expectedGreeting = MessageTemplate.createGreeting(memberName);
        return message.startsWith(expectedGreeting);
    }
}