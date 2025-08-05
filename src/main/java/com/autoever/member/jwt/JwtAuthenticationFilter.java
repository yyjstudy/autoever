package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 토큰을 검증하고 Spring Security Context에 인증 정보를 설정하는 필터
 * HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출하여 유효성을 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtUtil.validateToken(token)) {
                setAuthentication(token);
                log.debug("JWT 토큰 인증 성공: {}", jwtUtil.extractUsername(token));
            } else if (token != null) {
                log.debug("유효하지 않은 JWT 토큰");
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     * Authorization 헤더에서 Bearer 토큰을 찾아 추출
     * 
     * @param request HTTP 요청
     * @return JWT 토큰 또는 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(JwtProperties.HEADER_STRING);
        return jwtUtil.extractTokenFromHeader(authorizationHeader);
    }
    
    /**
     * JWT 토큰에서 추출한 사용자 정보로 Spring Security Context 설정
     * 
     * @param token 유효한 JWT 토큰
     */
    private void setAuthentication(String token) {
        String username = jwtUtil.extractUsername(token);
        
        // 기본 사용자 권한 설정 (추후 토큰에서 권한 정보를 추출하도록 확장 가능)
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_USER")
        );
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(username, null, authorities);
            
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    /**
     * 특정 경로는 JWT 필터를 적용하지 않음
     * 
     * @param request HTTP 요청
     * @return 필터 적용 제외 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        
        // 인증이 필요없는 경로들
        return path.equals("/api/users/register") ||
               path.equals("/api/users/login") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/h2-console/") ||
               path.equals("/");
    }
}