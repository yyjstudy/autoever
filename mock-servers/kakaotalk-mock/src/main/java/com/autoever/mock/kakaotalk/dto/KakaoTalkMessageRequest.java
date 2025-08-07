package com.autoever.mock.kakaotalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * KakaoTalk API 메시지 요청 DTO
 */
@Data
public class KakaoTalkMessageRequest {
    
    @JsonProperty("phone")
    private String phoneNumber;
    
    @JsonProperty("message")
    private String message;
}