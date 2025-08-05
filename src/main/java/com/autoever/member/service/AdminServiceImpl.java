package com.autoever.member.service;

import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.UserNotFoundException;
import com.autoever.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 서비스 구현체
 * 관리자 권한이 필요한 회원 관리 기능의 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    /**
     * 전체 회원 목록을 페이징하여 조회
     * 
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 회원 목록 (민감정보 마스킹 처리)
     */
    @Override
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
    @Override
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
}