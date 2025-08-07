package com.autoever.mock.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SMS API 메시지 요청 DTO (Immutable Record)
 */
public record SmsRequest(
    @JsonProperty("message")
    String message
) {}