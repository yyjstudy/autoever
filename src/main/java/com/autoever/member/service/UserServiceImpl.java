package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 관련 비즈니스 로직 구현체
 * Task 4.3에서 상세 로직 구현 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Override
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        // TODO: Task 4.3에서 구현 예정
        // - 사용자명 중복 체크
        // - 주민등록번호 중복 체크  
        // - 비밀번호 BCrypt 해싱
        // - User 엔티티 저장
        // - UserResponseDto 반환 (비밀번호 제외)
        
        log.info("UserService.registerUser() - Task 4.3에서 구현 예정");
        throw new UnsupportedOperationException("Task 4.3에서 구현 예정");
    }
}