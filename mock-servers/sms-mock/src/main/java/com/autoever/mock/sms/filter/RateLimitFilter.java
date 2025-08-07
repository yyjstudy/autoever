package com.autoever.mock.sms.filter;

import com.autoever.mock.sms.dto.SmsResponse;
import com.autoever.mock.sms.service.SimpleRateLimiter;
import com.autoever.mock.sms.service.SimpleRateLimiter.RateLimitInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting 필터 - 튜토리얼 수준
 * SMS Mock API 요청에 Rate Limiting 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final SimpleRateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        // SMS API 엔드포인트만 Rate Limiting 적용
        if (requestUri.startsWith("/sms")) {
            
            RateLimitInfo rateLimitInfo = rateLimiter.getCurrentUsage();
            
            // Rate Limit 체크
            if (!rateLimiter.tryAcquire()) {
                log.warn("SMS Rate Limit 초과 - URI: {}, 현재 사용량: {}", 
                         requestUri, rateLimitInfo.currentUsage());
                
                // 요구사항 준수: 429 대신 500 사용 (Internal Server Error)
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                SmsResponse errorResponse = SmsResponse.failure(
                    "RATE_LIMIT_EXCEEDED", 
                    "Rate limit exceeded. Server temporarily unavailable."
                );
                
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                
                // Rate Limit 정보 헤더 추가
                addRateLimitHeaders(response, rateLimitInfo);
                return;
            }
            
            // 정상 요청 시에도 Rate Limit 정보 헤더 추가
            addRateLimitHeaders(response, rateLimitInfo);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Rate Limit 정보를 HTTP 헤더에 추가
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitInfo rateLimitInfo) {
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitInfo.remainingRequests()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitInfo.resetTimeMillis()));
        response.setHeader("X-RateLimit-Reset-After", String.valueOf(rateLimitInfo.getRemainingTimeSeconds()));
    }
}