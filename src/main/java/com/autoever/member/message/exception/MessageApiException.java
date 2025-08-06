package com.autoever.member.message.exception;

import com.autoever.member.message.ApiType;

/**
 * 메시지 API 관련 기본 예외 클래스
 */
public class MessageApiException extends RuntimeException {
    
    private final ApiType apiType;
    private final String errorCode;
    
    public MessageApiException(ApiType apiType, String errorCode, String message) {
        super(message);
        this.apiType = apiType;
        this.errorCode = errorCode;
    }
    
    public MessageApiException(ApiType apiType, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.apiType = apiType;
        this.errorCode = errorCode;
    }
    
    public ApiType getApiType() {
        return apiType;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}