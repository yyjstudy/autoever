package com.autoever.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정 프로퍼티
 * application.yml의 jwt 설정값을 바인딩
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secretKey,
    long expirationTime,
    String tokenPrefix
) {
    /**
     * Authorization 헤더 이름
     */
    public static final String HEADER_STRING = "Authorization";
    
    /**
     * 토큰 프리픽스와 공백을 포함한 전체 프리픽스 반환
     */
    public String getTokenPrefixWithSpace() {
        return tokenPrefix + " ";
    }
}