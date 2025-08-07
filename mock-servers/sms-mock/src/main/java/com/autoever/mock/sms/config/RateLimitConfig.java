package com.autoever.mock.sms.config;

import com.autoever.mock.sms.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate Limiting 설정 - SMS Mock 서버
 * 튜토리얼 수준의 간단한 필터 등록
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {
    
    private final RateLimitFilter rateLimitFilter;
    
    /**
     * Rate Limiting 필터를 Spring Boot에 등록
     * SMS API 엔드포인트에만 적용
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/sms/*"); // SMS API 경로만
        registration.setOrder(1); // 인증 필터보다 먼저 실행
        registration.setName("RateLimitFilter");
        
        return registration;
    }
}