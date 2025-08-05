package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicateAccountException;
import com.autoever.member.exception.DuplicateSocialNumberException;
import com.autoever.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직 구현체
 * 회원가입, 중복 검증, 비밀번호 해싱 등 핵심 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 회원가입 처리
     * 
     * @param registrationDto 회원가입 요청 데이터
     * @return 생성된 사용자 정보 (민감정보 마스킹 처리)
     * @throws DuplicateAccountException 사용자명 중복 시
     * @throws DuplicateSocialNumberException 주민등록번호 중복 시
     */
    @Override
    @Transactional
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        log.info("회원가입 처리 시작: username={}", registrationDto.username());

        // 1. 사용자명 중복 체크
        validateUniqueUsername(registrationDto.username());

        // 2. 주민등록번호 중복 체크
        validateUniqueSocialNumber(registrationDto.socialNumber());

        // 3. 비밀번호 해싱
        String hashedPassword = passwordEncoder.encode(registrationDto.password());
        log.debug("비밀번호 해싱 완료: username={}", registrationDto.username());

        // 4. User 엔티티 생성 및 저장
        User user = createUserEntity(registrationDto, hashedPassword);
        User savedUser = userRepository.save(user);
        
        log.info("회원가입 완료: userId={}, username={}", savedUser.getId(), savedUser.getUsername());

        // 5. UserResponseDto 반환 (민감정보 마스킹)
        return UserResponseDto.from(savedUser);
    }

    /**
     * 사용자명 중복 검증
     * 
     * @param username 검증할 사용자명
     * @throws DuplicateAccountException 사용자명이 중복된 경우
     */
    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("사용자명 중복 검출: {}", username);
            throw new DuplicateAccountException(username);
        }
        log.debug("사용자명 중복 검증 통과: {}", username);
    }

    /**
     * 주민등록번호 중복 검증
     * 
     * @param socialNumber 검증할 주민등록번호
     * @throws DuplicateSocialNumberException 주민등록번호가 중복된 경우
     */
    private void validateUniqueSocialNumber(String socialNumber) {
        if (userRepository.existsBySocialNumber(socialNumber)) {
            log.warn("주민등록번호 중복 검출: {}", 
                socialNumber.substring(0, 6) + "-*******"); // 로그에서 마스킹
            throw new DuplicateSocialNumberException();
        }
        log.debug("주민등록번호 중복 검증 통과");
    }

    /**
     * UserRegistrationDto를 기반으로 User 엔티티 생성
     * 
     * @param registrationDto 회원가입 요청 데이터
     * @param hashedPassword 해싱된 비밀번호
     * @return 생성된 User 엔티티
     */
    private User createUserEntity(UserRegistrationDto registrationDto, String hashedPassword) {
        return User.builder()
                .username(registrationDto.username())
                .password(hashedPassword)
                .name(registrationDto.name())
                .socialNumber(registrationDto.socialNumber())
                .email(registrationDto.email())
                .phoneNumber(registrationDto.phoneNumber())
                .address(registrationDto.address())
                .build();
    }
}