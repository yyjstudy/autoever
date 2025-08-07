package com.autoever.mock.kakaotalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KakaoTalk API 메시지 응답 DTO (Immutable Record)
 */
public record KakaoTalkMessageResponse(
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
    public static KakaoTalkMessageResponse success(String messageId) {
        return new KakaoTalkMessageResponse(
            "OK",
            messageId,
            System.currentTimeMillis(),
            null,
            null
        );
    }
    
    // 실패 응답 생성
    public static KakaoTalkMessageResponse failure(String errorCode, String error) {
        return new KakaoTalkMessageResponse(
            "ERROR",
            null,
            System.currentTimeMillis(),
            error,
            errorCode
        );
    }
}