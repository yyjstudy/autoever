package com.autoever.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 보안 관련 설정 클래스
 * 비밀번호 인코딩 및 보안 정책 관리
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 패스워드 인코더 Bean 등록
     * 비밀번호 해싱에 사용되는 BCrypt 알고리즘 설정
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}