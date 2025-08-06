package com.autoever.member.message.dto;

import com.autoever.member.message.ApiType;

import java.time.LocalDateTime;

/**
 * 메시지 발송 응답 DTO
 */
public record MessageResponse(
    boolean success,
    String messageId,
    String errorCode,
    String errorMessage,
    LocalDateTime timestamp,
    ApiType apiType
) {
    
    public static MessageResponse success(String messageId, ApiType apiType) {
        return new MessageResponse(
            true,
            messageId,
            null,
            null,
            LocalDateTime.now(),
            apiType
        );
    }
    
    public static MessageResponse failure(String errorCode, String errorMessage, ApiType apiType) {
        return new MessageResponse(
            false,
            null,
            errorCode,
            errorMessage,
            LocalDateTime.now(),
            apiType
        );
    }
}