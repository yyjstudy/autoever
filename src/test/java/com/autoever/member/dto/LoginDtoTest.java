package com.autoever.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginDto 검증 테스트")
class LoginDtoTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    @DisplayName("유효한 로그인 정보로 DTO 생성")
    void validLoginDto() {
        // Given
        LoginDto loginDto = new LoginDto("testuser", "Password123!");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).isEmpty();
        assertThat(loginDto.username()).isEqualTo("testuser");
        assertThat(loginDto.password()).isEqualTo("Password123!");
    }
    
    @Test
    @DisplayName("사용자명이 null인 경우 검증 실패")
    void usernameNull_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto(null, "Password123!");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("사용자명은 필수입니다");
    }
    
    @Test
    @DisplayName("사용자명이 빈 문자열인 경우 검증 실패")
    void usernameBlank_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto("", "Password123!");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(2); // @NotBlank, @Size
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .containsExactlyInAnyOrder(
                        "사용자명은 필수입니다",
                        "사용자명은 3-50자 사이여야 합니다"
                );
    }
    
    @Test
    @DisplayName("사용자명이 너무 짧은 경우 검증 실패")
    void usernameTooShort_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto("ab", "Password123!");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("사용자명은 3-50자 사이여야 합니다");
    }
    
    @Test
    @DisplayName("비밀번호가 null인 경우 검증 실패")
    void passwordNull_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto("testuser", null);
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("비밀번호는 필수입니다");
    }
    
    @Test
    @DisplayName("비밀번호가 빈 문자열인 경우 검증 실패")
    void passwordBlank_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto("testuser", "");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(2); // @NotBlank, @Size
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .containsExactlyInAnyOrder(
                        "비밀번호는 필수입니다",
                        "비밀번호는 8-100자 사이여야 합니다"
                );
    }
    
    @Test
    @DisplayName("비밀번호가 너무 짧은 경우 검증 실패")
    void passwordTooShort_ValidationFails() {
        // Given
        LoginDto loginDto = new LoginDto("testuser", "Pass1!");
        
        // When
        Set<ConstraintViolation<LoginDto>> violations = validator.validate(loginDto);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("비밀번호는 8-100자 사이여야 합니다");
    }
}