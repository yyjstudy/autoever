package com.autoever.member.message.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 메시지 발송 과정을 추적하기 위한 MDC(Mapped Diagnostic Context) 유틸리티
 */
public class MessageTraceContext {
    
    private static final String TRACE_ID_KEY = "traceId";
    private static final String PHONE_NUMBER_KEY = "phoneNumber";
    private static final String MESSAGE_TYPE_KEY = "messageType";
    private static final String API_TYPE_KEY = "apiType";
    
    /**
     * 새로운 트레이스 컨텍스트를 시작합니다.
     * 
     * @param phoneNumber 전화번호 (마스킹됨)
     * @param messageType 메시지 타입 (예: template, direct)
     * @return 생성된 트레이스 ID
     */
    public static String startTrace(String phoneNumber, String messageType) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(PHONE_NUMBER_KEY, phoneNumber);
        MDC.put(MESSAGE_TYPE_KEY, messageType);
        
        return traceId;
    }
    
    /**
     * 현재 API 타입을 설정합니다.
     * 
     * @param apiType API 타입
     */
    public static void setApiType(String apiType) {
        MDC.put(API_TYPE_KEY, apiType);
    }
    
    /**
     * 현재 트레이스 ID를 반환합니다.
     * 
     * @return 트레이스 ID (없으면 null)
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * 현재 전화번호를 반환합니다.
     * 
     * @return 전화번호 (없으면 null)
     */
    public static String getCurrentPhoneNumber() {
        return MDC.get(PHONE_NUMBER_KEY);
    }
    
    /**
     * 현재 메시지 타입을 반환합니다.
     * 
     * @return 메시지 타입 (없으면 null)
     */
    public static String getCurrentMessageType() {
        return MDC.get(MESSAGE_TYPE_KEY);
    }
    
    /**
     * 현재 API 타입을 반환합니다.
     * 
     * @return API 타입 (없으면 null)
     */
    public static String getCurrentApiType() {
        return MDC.get(API_TYPE_KEY);
    }
    
    /**
     * 트레이스 컨텍스트를 종료하고 MDC를 정리합니다.
     */
    public static void endTrace() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(PHONE_NUMBER_KEY);
        MDC.remove(MESSAGE_TYPE_KEY);
        MDC.remove(API_TYPE_KEY);
    }
    
    /**
     * 현재 컨텍스트 정보를 문자열로 반환합니다.
     * 
     * @return 컨텍스트 정보
     */
    public static String getContextInfo() {
        return String.format("traceId=%s, phone=%s, messageType=%s, apiType=%s",
            getCurrentTraceId(),
            getCurrentPhoneNumber(),
            getCurrentMessageType(),
            getCurrentApiType());
    }
}