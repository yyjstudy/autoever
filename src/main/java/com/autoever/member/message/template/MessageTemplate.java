package com.autoever.member.message.template;

/**
 * 메시지 템플릿 클래스
 * 모든 메시지의 시작 부분에 표준 인사말을 추가합니다.
 */
public class MessageTemplate {
    
    // 표준 인사말 템플릿 - {memberName}은 실제 회원 이름으로 대체됩니다
    private static final String GREETING_TEMPLATE = "{memberName}님, 안녕하세요. 현대 오토에버입니다.";
    
    // 메시지 구분자
    private static final String MESSAGE_SEPARATOR = "\n\n";
    
    /**
     * 표준 인사말을 메시지 앞에 추가합니다.
     * 
     * @param memberName 회원 이름
     * @param originalMessage 원본 메시지
     * @return 템플릿이 적용된 전체 메시지
     */
    public static String applyTemplate(String memberName, String originalMessage) {
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new IllegalArgumentException("회원 이름은 필수입니다.");
        }
        
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다.");
        }
        
        String greeting = GREETING_TEMPLATE.replace("{memberName}", memberName.trim());
        return greeting + MESSAGE_SEPARATOR + originalMessage.trim();
    }
    
    /**
     * 인사말 템플릿만 생성합니다.
     * 
     * @param memberName 회원 이름
     * @return 인사말 텍스트
     */
    public static String createGreeting(String memberName) {
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new IllegalArgumentException("회원 이름은 필수입니다.");
        }
        
        return GREETING_TEMPLATE.replace("{memberName}", memberName.trim());
    }
}