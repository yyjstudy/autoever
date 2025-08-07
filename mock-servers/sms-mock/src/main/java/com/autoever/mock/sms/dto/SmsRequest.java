package com.autoever.mock.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SMS API 메시지 요청 DTO
 */
@Data
public class SmsRequest {
    
    @JsonProperty("message")
    private String message;
}