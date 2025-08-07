package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 생성 및 검증 유틸리티 클래스
 * JJWT 라이브러리를 사용하여 JWT 토큰 관련 기능 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtProperties jwtProperties;
    
    /**
     * 사용자명을 기반으로 JWT 토큰 생성
     * 
     * @param username 사용자명
     * @return 생성된 JWT 토큰
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    /**
     * 추가 클레임과 함께 JWT 토큰 생성
     * 
     * @param claims 추가 클레임
     * @param username 사용자명
     * @return 생성된 JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.expirationTime());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * JWT 토큰에서 사용자명 추출
     * 
     * @param token JWT 토큰
     * @return 사용자명
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * JWT 토큰에서 만료 시간 추출
     * 
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * JWT 토큰에서 특정 클레임 추출
     * 
     * @param token JWT 토큰
     * @param claimsResolver 클레임 추출 함수
     * @return 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * JWT 토큰에서 모든 클레임 추출
     * 
     * @param token JWT 토큰
     * @return 모든 클레임
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * JWT 토큰이 만료되었는지 확인
     * 
     * @param token JWT 토큰
     * @return 만료 여부
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * JWT 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @param username 검증할 사용자명
     * @return 토큰 유효성 여부
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * JWT 토큰 유효성 검증 (사용자명 검증 없이)
     * 
     * @param token JWT 토큰
     * @return 토큰 유효성 여부
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * JWT 서명에 사용할 키 생성
     * 
     * @return 서명 키
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Authorization 헤더에서 JWT 토큰 추출
     * 
     * @param authorizationHeader Authorization 헤더 값
     * @return JWT 토큰 (Bearer 프리픽스 제거됨)
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(jwtProperties.tokenPrefix() + " ")) {
            return authorizationHeader.substring(jwtProperties.tokenPrefix().length() + 1);
        }
        return null;
    }
    
    /**
     * JWT 토큰 만료 시간 (밀리초) 반환
     * 
     * @return 토큰 만료 시간 (밀리초)
     */
    public long getExpirationTime() {
        return jwtProperties.expirationTime();
    }
}