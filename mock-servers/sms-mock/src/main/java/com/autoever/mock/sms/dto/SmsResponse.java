package com.autoever.mock.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SMS API 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsResponse {
    
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
    public static SmsResponse success(String messageId) {
        SmsResponse response = new SmsResponse();
        response.setResult("OK");
        response.setMessageId(messageId);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 실패 응답 생성
    public static SmsResponse failure(String errorCode, String error) {
        SmsResponse response = new SmsResponse();
        response.setResult("ERROR");
        response.setErrorCode(errorCode);
        response.setError(error);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}