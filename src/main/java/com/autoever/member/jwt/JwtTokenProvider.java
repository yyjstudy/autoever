package com.autoever.member.jwt;

import com.autoever.member.dto.JwtTokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성 및 관리 Provider
 * 인증 성공 시 JWT 토큰을 생성하고 클라이언트에게 반환하는 로직을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final JwtUtil jwtUtil;
    
    /**
     * Authentication 객체로부터 JWT 토큰을 생성
     * 
     * @param authentication Spring Security Authentication 객체
     * @return JWT 토큰 응답 DTO
     */
    public JwtTokenDto generateToken(Authentication authentication) {
        String username = authentication.getName();
        log.debug("JWT 토큰 생성 시작: username={}", username);
        
        // JWT 토큰 생성
        String token = jwtUtil.generateToken(username);
        long expiresIn = jwtUtil.getExpirationTime();
        
        log.info("JWT 토큰 생성 완료: username={}, expiresIn={}ms", username, expiresIn);
        
        return JwtTokenDto.of(token, expiresIn);
    }
    
    /**
     * 사용자명으로부터 직접 JWT 토큰을 생성
     * 
     * @param username 사용자명
     * @return JWT 토큰 응답 DTO
     */
    public JwtTokenDto generateTokenForUsername(String username) {
        log.debug("사용자명으로 JWT 토큰 생성 시작: username={}", username);
        
        // JWT 토큰 생성
        String token = jwtUtil.generateToken(username);
        long expiresIn = jwtUtil.getExpirationTime();
        
        log.info("JWT 토큰 생성 완료: username={}, expiresIn={}ms", username, expiresIn);
        
        return JwtTokenDto.of(token, expiresIn);
    }
    
    /**
     * JWT 토큰의 유효성을 검증
     * 
     * @param token JWT 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
    
    /**
     * JWT 토큰에서 사용자명을 추출
     * 
     * @param token JWT 토큰
     * @return 사용자명
     */
    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    /**
     * JWT 토큰의 만료 시간을 반환
     * 
     * @return 만료 시간 (밀리초)
     */
    public long getTokenExpirationTime() {
        return jwtUtil.getExpirationTime();
    }
}