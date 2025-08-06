package com.autoever.member.message;

/**
 * 메시지 API 타입 열거형
 * 지원하는 외부 메시지 서비스 타입을 정의합니다.
 */
public enum ApiType {
    KAKAOTALK("카카오톡", 100),  // 100회/분 제한
    SMS("SMS", 500);            // 500회/분 제한

    private final String displayName;
    private final int rateLimit; // 분당 요청 제한

    ApiType(String displayName, int rateLimit) {
        this.displayName = displayName;
        this.rateLimit = rateLimit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRateLimit() {
        return rateLimit;
    }
}