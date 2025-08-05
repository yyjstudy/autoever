package com.autoever.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JWT 토큰 응답 DTO
 * 로그인 성공 시 반환되는 JWT 토큰 정보
 */
@Schema(description = "JWT 토큰 응답 정보")
public record JwtTokenDto(
    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    
    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType,
    
    @Schema(description = "토큰 만료 시간 (초)", example = "3600")
    long expiresIn
) {
    /**
     * JWT 토큰 생성 팩토리 메서드
     * 
     * @param token JWT 토큰
     * @param expiresInMillis 만료 시간 (밀리초)
     * @return JwtTokenDto 인스턴스
     */
    public static JwtTokenDto of(String token, long expiresInMillis) {
        return new JwtTokenDto(
            token,
            "Bearer",
            expiresInMillis / 1000 // 밀리초를 초로 변환
        );
    }
}