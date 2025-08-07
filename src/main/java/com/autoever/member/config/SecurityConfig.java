package com.autoever.member.config;

import com.autoever.member.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 보안 설정 클래스
 * HTTP 보안, 인증, 권한 부여 및 보안 헤더 관리
 * JWT와 Basic Authentication을 모두 지원하는 하이브리드 인증 시스템
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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


    /**
     * Spring Security 필터체인 설정
     * HTTP 보안 정책, 인증 방식, 권한 부여 규칙을 정의
     * 
     * @param http HttpSecurity 설정 객체
     * @return SecurityFilterChain 보안 필터체인
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 보안 헤더 설정 (Spring Boot 3.x 최신 방식)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())  // X-Frame-Options: DENY
                .contentTypeOptions(contentTypeOptions -> {})  // X-Content-Type-Options: nosniff (자동 활성화)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)  // HSTS 1년
                )
                .referrerPolicy(referrerPolicy -> 
                    referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            
            // 경로별 권한 설정 및 접근 제어 구성
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 허용 경로
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                
                // 테스트 API는 관리자 권한 필요 (H2 메모리 DB 테스트용)
                .requestMatchers("/api/test/**").hasRole("ADMIN")
                
                // 관리자 전용 API는 ADMIN 권한 필요
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Swagger UI 및 API 문서는 인증 필요
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                
                // H2 콘솔은 인증 필요
                .requestMatchers("/h2-console/**").authenticated()
                
                // 기타 모든 API 요청은 인증 필요
                .requestMatchers("/api/**").authenticated()
                
                // 나머지 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // CSRF 비활성화 (REST API이므로 불필요)
            .csrf(csrf -> csrf.disable())
            
            // 세션 관리: JWT 기반 무상태 인증을 위해 STATELESS 설정
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // JWT 기반 무상태 인증
            )
            
            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Basic Authentication 활성화 (관리자 접근용)
            .httpBasic(httpBasic -> {})  // 기본 HTTP Basic 인증 활성화
            
            // 폼 로그인 비활성화 (JWT 기반 API이므로 불필요)
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
    
    /**
     * AuthenticationProvider Bean 설정
     * UserDetailsService와 PasswordEncoder를 사용하여 인증 처리
     * 
     * @param userDetailsService 사용자 정보 서비스 (@Primary로 CompositeUserDetailsService가 주입됨)
     * @param passwordEncoder 패스워드 인코더
     * @return DaoAuthenticationProvider 인스턴스
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, 
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    /**
     * AuthenticationManager Bean 설정
     * 로그인 시 사용자 인증을 처리하는 관리자
     * 
     * @param authConfig Spring Security 인증 설정
     * @return AuthenticationManager 인스턴스
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
}