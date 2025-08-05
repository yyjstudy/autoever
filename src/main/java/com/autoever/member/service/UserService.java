package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * 사용자 회원가입 처리
     * 
     * @param registrationDto 회원가입 요청 데이터
     * @return 생성된 사용자 정보 (비밀번호 제외)
     * @throws DuplicateAccountException 사용자명 중복 시
     * @throws DuplicateSocialNumberException 주민등록번호 중복 시
     */
    UserResponseDto registerUser(UserRegistrationDto registrationDto);
}