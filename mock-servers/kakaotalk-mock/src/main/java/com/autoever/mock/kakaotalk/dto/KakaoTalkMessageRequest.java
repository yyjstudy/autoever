package com.autoever.mock.kakaotalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KakaoTalk API 메시지 요청 DTO (Immutable Record)
 */
public record KakaoTalkMessageRequest(
    @JsonProperty("phone")
    String phoneNumber,
    
    @JsonProperty("message")
    String message
) {}