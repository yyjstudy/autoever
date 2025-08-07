package com.autoever.mock.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SMS API 응답 DTO (Immutable Record)
 */
public record SmsResponse(
    @JsonProperty("result")
    String result,
    
    @JsonProperty("messageId")
    String messageId,
    
    @JsonProperty("timestamp")
    Long timestamp,
    
    @JsonProperty("error")
    String error,
    
    @JsonProperty("errorCode")
    String errorCode
) {
    
    // 성공 응답 생성
    public static SmsResponse success(String messageId) {
        return new SmsResponse(
            "OK",
            messageId,
            System.currentTimeMillis(),
            null,
            null
        );
    }
    
    // 실패 응답 생성
    public static SmsResponse failure(String errorCode, String error) {
        return new SmsResponse(
            "ERROR",
            null,
            System.currentTimeMillis(),
            error,
            errorCode
        );
    }
}