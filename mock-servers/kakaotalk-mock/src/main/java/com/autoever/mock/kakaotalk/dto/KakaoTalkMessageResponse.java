package com.autoever.mock.kakaotalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KakaoTalk API 메시지 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoTalkMessageResponse {
    
    @JsonProperty("result")
    private String result;
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    // 성공 응답 생성
    public static KakaoTalkMessageResponse success(String messageId) {
        KakaoTalkMessageResponse response = new KakaoTalkMessageResponse();
        response.setResult("OK");
        response.setMessageId(messageId);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 실패 응답 생성
    public static KakaoTalkMessageResponse failure(String errorCode, String error) {
        KakaoTalkMessageResponse response = new KakaoTalkMessageResponse();
        response.setResult("ERROR");
        response.setErrorCode(errorCode);
        response.setError(error);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}