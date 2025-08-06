package com.autoever.member.message.client;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;

/**
 * 메시지 API 클라이언트 인터페이스
 * 다양한 메시지 서비스(카카오톡, SMS 등)에 대한 공통 인터페이스를 정의합니다.
 */
public interface MessageApiClient {
    
    /**
     * 메시지 발송
     * 
     * @param request 메시지 발송 요청
     * @return 발송 결과
     */
    MessageResponse sendMessage(MessageRequest request);
    
    /**
     * API 사용 가능 여부 확인
     * 
     * @return 사용 가능 여부
     */
    boolean isAvailable();
    
    /**
     * API 타입 반환
     * 
     * @return API 타입
     */
    ApiType getApiType();
    
    /**
     * 연결 상태 확인
     * 
     * @return 연결 상태 (true: 정상, false: 연결 불가)
     */
    boolean validateConnection();
}