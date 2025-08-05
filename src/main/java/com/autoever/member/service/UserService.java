package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicateAccountException;
import com.autoever.member.exception.DuplicateEmailException;
import com.autoever.member.exception.DuplicatePhoneNumberException;
import com.autoever.member.exception.DuplicateSocialNumberException;
import com.autoever.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직 서비스
 * 회원가입, 중복 검증, 비밀번호 해싱 등 핵심 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 회원가입 처리
     * 
     * @param registrationDto 회원가입 요청 데이터
     * @return 생성된 사용자 정보 (민감정보 마스킹 처리)
     * @throws DuplicateAccountException 사용자명 중복 시
     * @throws DuplicateSocialNumberException 주민등록번호 중복 시
     * @throws DuplicateEmailException 이메일 중복 시
     * @throws DuplicatePhoneNumberException 전화번호 중복 시
     */
    @Transactional
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        log.info("회원가입 처리 시작: username={}", registrationDto.username());

        // 1. 사용자명 중복 체크
        validateUniqueUsername(registrationDto.username());

        // 2. 주민등록번호 중복 체크
        validateUniqueSocialNumber(registrationDto.socialNumber());

        // 3. 이메일 중복 체크
        validateUniqueEmail(registrationDto.email());

        // 4. 전화번호 중복 체크
        validateUniquePhoneNumber(registrationDto.phoneNumber());

        // 5. 비밀번호 해싱
        String hashedPassword = passwordEncoder.encode(registrationDto.password());
        log.debug("비밀번호 해싱 완료: username={}", registrationDto.username());

        // 6. User 엔티티 생성 및 저장
        User user = createUserEntity(registrationDto, hashedPassword);
        User savedUser = userRepository.save(user);
        
        log.info("회원가입 완료: userId={}, username={}", savedUser.getId(), savedUser.getUsername());

        // 7. UserResponseDto 반환 (민감정보 마스킹)
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
     * 이메일 중복 검증
     * 
     * @param email 검증할 이메일 주소
     * @throws DuplicateEmailException 이메일이 중복된 경우
     */
    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("이메일 중복 검출: {}", maskEmailForLog(email));
            throw new DuplicateEmailException(email);
        }
        log.debug("이메일 중복 검증 통과: {}", email);
    }

    /**
     * 전화번호 중복 검증
     * 
     * @param phoneNumber 검증할 전화번호
     * @throws DuplicatePhoneNumberException 전화번호가 중복된 경우
     */
    private void validateUniquePhoneNumber(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("전화번호 중복 검출: {}", maskPhoneNumberForLog(phoneNumber));
            throw new DuplicatePhoneNumberException(phoneNumber);
        }
        log.debug("전화번호 중복 검증 통과: {}", phoneNumber);
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

    /**
     * 로그용 이메일 마스킹 처리
     * 
     * @param email 원본 이메일
     * @return 마스킹된 이메일
     */
    private String maskEmailForLog(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() <= 1) {
            return "*@" + domainPart;
        } else if (localPart.length() <= 3) {
            return localPart.charAt(0) + "**@" + domainPart;
        } else {
            return localPart.charAt(0) + "***@" + domainPart;
        }
    }

    /**
     * 로그용 전화번호 마스킹 처리
     * 
     * @param phoneNumber 원본 전화번호
     * @return 마스킹된 전화번호
     */
    private String maskPhoneNumberForLog(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        
        // 010-1234-5678 형식인 경우
        if (phoneNumber.contains("-") && phoneNumber.length() == 13) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        // 01012345678 형식인 경우
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        
        // 기타 형식
        return "***-****-****";
    }
}