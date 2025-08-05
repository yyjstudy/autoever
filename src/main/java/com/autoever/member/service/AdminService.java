package com.autoever.member.service;

import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.dto.UserUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 전용 서비스 인터페이스
 * 관리자 권한이 필요한 회원 관리 기능을 제공
 */
public interface AdminService {
    
    /**
     * 전체 회원 목록을 페이징하여 조회
     * 
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 회원 목록 (민감정보 마스킹 처리)
     */
    Page<UserResponseDto> getAllUsers(Pageable pageable);
    
    /**
     * 특정 회원의 상세 정보를 조회
     * 
     * @param id 조회할 회원의 ID
     * @return 회원 상세 정보 (민감정보 마스킹 처리)
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    UserResponseDto getUserById(Long id);
    
    /**
     * 회원 정보를 수정
     * 
     * @param id 수정할 회원의 ID
     * @param updateDto 수정할 정보 (암호/주소 선택적 필드)
     * @return 수정된 회원 정보 (민감정보 마스킹 처리)
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    UserResponseDto updateUser(Long id, UserUpdateDto updateDto);
    
    /**
     * 회원을 삭제
     * 
     * @param id 삭제할 회원의 ID
     * @throws UserNotFoundException 회원을 찾을 수 없는 경우
     */
    void deleteUser(Long id);
}