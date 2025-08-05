package com.autoever.member.service;

import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.dto.UserUpdateDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicatePhoneNumberException;
import com.autoever.member.exception.UserNotFoundException;
import com.autoever.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 서비스
 * 관리자 권한이 필요한 회원 관리 기능의 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 전체 회원 목록을 페이징하여 조회
     * 
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 회원 목록 (민감정보 마스킹 처리)
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        log.info("관리자 전체 회원 목록 조회 시작: page={}, size={}, sort={}", 
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        
        Page<User> users = userRepository.findAll(pageable);
        
        log.info("관리자 전체 회원 목록 조회 완료: totalElements={}, totalPages={}, currentPage={}", 
            users.getTotalElements(), users.getTotalPages(), users.getNumber());
        
        // User 엔티티를 UserResponseDto로 변환 (민감정보 마스킹 처리)
        return users.map(UserResponseDto::from);
    }

    /**
     * 특정 회원의 상세 정보를 조회
     * 
     * @param id 조회할 회원의 ID
     * @return 회원 상세 정보 (민감정보 마스킹 처리)
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        log.info("관리자 회원 상세 조회 시작: userId={}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("회원 조회 실패: userId={} 존재하지 않음", id);
                return UserNotFoundException.byId(id);
            });
        
        log.info("관리자 회원 상세 조회 완료: userId={}, username={}", user.getId(), user.getUsername());
        
        // User 엔티티를 UserResponseDto로 변환 (민감정보 마스킹 처리)
        return UserResponseDto.from(user);
    }

    /**
     * 회원 정보를 수정
     * 
     * @param id 수정할 회원의 ID
     * @param updateDto 수정할 정보 (암호/주소 선택적 필드)
     * @return 수정된 회원 정보 (민감정보 마스킹 처리)
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateDto updateDto) {
        log.info("관리자 회원 정보 수정 시작: userId={}", id);
        
        // 수정할 필드가 있는지 확인
        if (!updateDto.hasAnyUpdate()) {
            log.warn("수정할 정보 없음: userId={}", id);
            throw new IllegalArgumentException("수정할 정보가 없습니다.");
        }
        
        // 기존 회원 조회
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("회원 수정 실패: userId={} 존재하지 않음", id);
                return UserNotFoundException.byId(id);
            });
        
        log.info("회원 수정 대상 확인: userId={}, username={}", user.getId(), user.getUsername());
        
        // 선택적 필드 업데이트 (암호와 주소만)
        updateUserFields(user, updateDto);
        
        // 변경사항 저장
        User updatedUser = userRepository.save(user);
        
        log.info("관리자 회원 정보 수정 완료: userId={}, updatedFields={}", 
            updatedUser.getId(), getUpdatedFields(updateDto));
        
        // UserResponseDto 변환 후 반환
        return UserResponseDto.from(updatedUser);
    }
    
    
    /**
     * 사용자 필드를 선택적으로 업데이트
     * 
     * @param user 업데이트할 User 엔티티
     * @param updateDto 업데이트 정보
     */
    private void updateUserFields(User user, UserUpdateDto updateDto) {
        if (updateDto.hasAddress()) {
            user.updateAddress(updateDto.address());
            log.debug("주소 업데이트: userId={}", user.getId());
        }
        
        if (updateDto.hasPassword()) {
            String hashedPassword = passwordEncoder.encode(updateDto.password());
            user.updatePassword(hashedPassword);
            log.debug("비밀번호 업데이트: userId={}", user.getId());
        }
    }
    
    /**
     * 업데이트된 필드 목록을 문자열로 반환 (로깅용)
     */
    private String getUpdatedFields(UserUpdateDto updateDto) {
        StringBuilder fields = new StringBuilder();
        if (updateDto.hasAddress()) fields.append("address,");
        if (updateDto.hasPassword()) fields.append("password,");
        
        return fields.length() > 0 
            ? fields.substring(0, fields.length() - 1) 
            : "none";
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

    /**
     * 회원을 삭제
     * 
     * @param id 삭제할 회원의 ID
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("관리자 회원 삭제 시작: userId={}", id);
        
        // 삭제할 회원이 존재하는지 확인
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("회원 삭제 실패: userId={} 존재하지 않음", id);
                return UserNotFoundException.byId(id);
            });
        
        String username = user.getUsername();
        log.info("회원 삭제 대상 확인: userId={}, username={}", user.getId(), username);
        
        // 회원 삭제 실행
        userRepository.delete(user);
        
        log.info("관리자 회원 삭제 완료: userId={}, username={}", id, username);
    }
}