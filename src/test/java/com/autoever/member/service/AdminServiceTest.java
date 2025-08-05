package com.autoever.member.service;

import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.dto.UserUpdateDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicatePhoneNumberException;
import com.autoever.member.exception.UserNotFoundException;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("전체 회원 목록 조회 성공")
    void getAllUsers_Success() {
        // Given
        User testUser = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("901010-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<UserResponseDto> result = adminService.getAllUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("testuser");
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("특정 회원 조회 성공")
    void getUserById_Success() {
        // Given
        User testUser = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("901010-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponseDto result = adminService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.phoneNumber()).isEqualTo("010-****-5678"); // 마스킹 확인
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("특정 회원 조회 실패 - 회원 없음")
    void getUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다. ID: 999");
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 비밀번호만 수정")
    void updateUser_PasswordOnly_Success() {
        // Given
        User testUser = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("901010-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();
        
        UserUpdateDto updateDto = new UserUpdateDto(null, "NewPassword123!");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponseDto result = adminService.updateUser(1L, updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testUser);
        verify(userRepository, never()).existsByPhoneNumber(anyString());
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 주소만 수정")
    void updateUser_AddressOnly_Success() {
        // Given
        User testUser = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("901010-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();
        
        UserUpdateDto updateDto = new UserUpdateDto("부산시 해운대구", null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponseDto result = adminService.updateUser(1L, updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(testUser);
        verify(passwordEncoder, never()).encode(anyString());
    }


    @Test
    @DisplayName("회원 정보 수정 실패 - 회원 없음")
    void updateUser_UserNotFound() {
        // Given
        UserUpdateDto updateDto = new UserUpdateDto("서울시", null);
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.updateUser(999L, updateDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다. ID: 999");
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 수정할 필드 없음")
    void updateUser_NoFieldsToUpdate() {
        // Given
        UserUpdateDto updateDto = new UserUpdateDto(null, null);

        // When & Then
        assertThatThrownBy(() -> adminService.updateUser(1L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수정할 정보가 없습니다.");
    }

    @Test
    @DisplayName("회원 삭제 성공")
    void deleteUser_Success() {
        // Given
        User testUser = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("901010-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        adminService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("회원 삭제 실패 - 회원 없음")
    void deleteUser_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다. ID: 999");
        
        verify(userRepository, never()).delete(any(User.class));
    }

}