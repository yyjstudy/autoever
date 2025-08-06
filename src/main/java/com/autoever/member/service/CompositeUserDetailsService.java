package com.autoever.member.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 복합 UserDetailsService 구현체
 * 인메모리 관리자 계정과 데이터베이스 일반 사용자를 모두 처리
 */
@Slf4j
@Service("compositeUserDetailsService")
@Primary  // 기본 UserDetailsService로 사용
public class CompositeUserDetailsService implements UserDetailsService {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    // 하드코딩된 관리자 계정
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "1212";
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("사용자 조회 시도: username={}", username);
        
        // 1. 관리자 계정 확인
        if (ADMIN_USERNAME.equals(username)) {
            log.debug("관리자 계정 조회 성공");
            // 매번 encode하지 않고 미리 인코딩된 값 사용
            return User.builder()
                    .username(ADMIN_USERNAME)
                    .password("$2a$10$rBeTLRBhB8.oXcUcY5c84OqhSr24pmDJXFZmJxhcsyL68isEhbFJm") // BCrypt로 인코딩된 "1212"
                    .roles("ADMIN", "USER")
                    .build();
        }
        
        // 2. 데이터베이스에서 일반 사용자 조회
        try {
            return customUserDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: username={}", username);
            throw e;
        }
    }
}