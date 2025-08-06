package com.autoever.member.message.exception;

import com.autoever.member.message.ApiType;

/**
 * API 연결 실패 예외
 */
public class ApiConnectionException extends MessageApiException {
    
    public ApiConnectionException(ApiType apiType, String message) {
        super(apiType, "CONNECTION_FAILED", message);
    }
    
    public ApiConnectionException(ApiType apiType, String message, Throwable cause) {
        super(apiType, "CONNECTION_FAILED", message, cause);
    }
}