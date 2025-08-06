package com.autoever.member.message.exception;

import com.autoever.member.message.ApiType;

/**
 * 메시지 발송 실패 예외
 */
public class MessageSendException extends MessageApiException {
    
    public MessageSendException(ApiType apiType, String errorCode, String message) {
        super(apiType, errorCode, message);
    }
    
    public MessageSendException(ApiType apiType, String errorCode, String message, Throwable cause) {
        super(apiType, errorCode, message, cause);
    }
}