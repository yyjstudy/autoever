package com.autoever.mock.sms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * SMS Mock 서버 보안 설정
 * Basic Authentication을 사용하여 API 접근을 제어합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (API 서버이므로)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 모든 요청에 대해 인증 요구
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/sms").authenticated()
                .anyRequest().permitAll()
            )
            
            // Basic Authentication 사용
            .httpBasic(withDefaults());

        return http.build();
    }
}