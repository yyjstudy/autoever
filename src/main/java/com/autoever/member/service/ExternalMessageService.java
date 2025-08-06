package com.autoever.member.service;

/**
 * 외부 메시지 발송 서비스 인터페이스
 */
public interface ExternalMessageService {
    
    /**
     * 메시지 발송
     * 
     * @param phoneNumber 수신자 전화번호
     * @param message 메시지 내용
     * @throws RuntimeException 발송 실패 시
     */
    void sendMessage(String phoneNumber, String message);
    
    /**
     * 메시지 서비스 가용성 확인
     * 
     * @return 가용 여부
     */
    boolean isAvailable();
}