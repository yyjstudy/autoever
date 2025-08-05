package com.autoever.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.Validation;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class UserDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("UserCreateDto 유효한 데이터 검증 테스트")
    void userCreateDtoValidDataTest() {
        // given
        UserCreateDto dto = new UserCreateDto(
            "testuser",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "010-1234-5678",
            "서울시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UserCreateDto 사용자명 검증 테스트")
    void userCreateDtoUsernameValidationTest() {
        // when - 사용자명이 null인 경우
        UserCreateDto dto = new UserCreateDto(null, "Password123!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("사용자명은 필수입니다");

        // when - 사용자명이 너무 짧은 경우
        dto = new UserCreateDto("ab", "Password123!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("사용자명은 3-50자 사이여야 합니다");

        // when - 사용자명이 너무 긴 경우
        dto = new UserCreateDto("a".repeat(51), "Password123!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("사용자명은 3-50자 사이여야 합니다");
    }

    @Test
    @DisplayName("UserCreateDto 비밀번호 검증 테스트")
    void userCreateDtoPasswordValidationTest() {
        // when - 비밀번호가 null인 경우
        UserCreateDto dto = new UserCreateDto("testuser", null, "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("비밀번호는 필수입니다");

        // when - 비밀번호가 너무 짧은 경우
        dto = new UserCreateDto("testuser", "Pass1!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("비밀번호는 최소 8자 이상이어야 합니다");

        // when - 비밀번호 패턴이 맞지 않는 경우 (대문자 없음)
        dto = new UserCreateDto("testuser", "password123!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다");

        // when - 비밀번호 패턴이 맞지 않는 경우 (소문자 없음)
        dto = new UserCreateDto("testuser", "PASSWORD123!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);

        // when - 비밀번호 패턴이 맞지 않는 경우 (숫자 없음)
        dto = new UserCreateDto("testuser", "Password!", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);

        // when - 비밀번호 패턴이 맞지 않는 경우 (특수문자 없음)
        dto = new UserCreateDto("testuser", "Password123", "홍길동", "901201-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("UserCreateDto 주민번호 형식 검증 테스트")
    void userCreateDtoSocialNumberValidationTest() {
        // when - 잘못된 형식 (하이픈 없음)
        UserCreateDto dto = new UserCreateDto("testuser", "Password123!", "홍길동", "9012011234567", "010-1234-5678", "서울시 강남구");
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("주민등록번호 형식이 올바르지 않습니다");

        // when - 앞자리 자릿수 틀림
        dto = new UserCreateDto("testuser", "Password123!", "홍길동", "90120-1234567", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);

        // when - 뒷자리 자릿수 틀림
        dto = new UserCreateDto("testuser", "Password123!", "홍길동", "901201-123456", "010-1234-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("UserCreateDto 전화번호 형식 검증 테스트")
    void userCreateDtoPhoneNumberValidationTest() {
        // when - 잘못된 형식 (하이픈 없음)
        UserCreateDto dto = new UserCreateDto("testuser", "Password123!", "홍길동", "901201-1234567", "01012345678", "서울시 강남구");
        Set<ConstraintViolation<UserCreateDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("전화번호 형식이 올바르지 않습니다");

        // when - 잘못된 형식 (중간자리 짧음)
        dto = new UserCreateDto("testuser", "Password123!", "홍길동", "901201-1234567", "010-123-5678", "서울시 강남구");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("UserUpdateDto 유효한 데이터 검증 테스트")
    void userUpdateDtoValidDataTest() {
        // given
        UserUpdateDto dto = new UserUpdateDto("부산시 해운대구 센텀로 456", null);

        // when
        Set<ConstraintViolation<UserUpdateDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PasswordChangeDto 유효한 데이터 검증 테스트")
    void passwordChangeDtoValidDataTest() {
        // given
        PasswordChangeDto dto = new PasswordChangeDto("oldPassword123!", "NewPassword456!", "NewPassword456!");

        // when
        Set<ConstraintViolation<PasswordChangeDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PasswordChangeDto 새 비밀번호 검증 테스트")
    void passwordChangeDtoNewPasswordValidationTest() {
        // when - 새 비밀번호가 null인 경우
        PasswordChangeDto dto = new PasswordChangeDto("oldPassword123!", null, "NewPassword456!");
        Set<ConstraintViolation<PasswordChangeDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("새 비밀번호는 필수입니다");

        // when - 새 비밀번호가 너무 짧은 경우
        dto = new PasswordChangeDto("oldPassword123!", "Pass1!", "NewPassword456!");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("비밀번호는 최소 8자 이상이어야 합니다");

        // when - 새 비밀번호 패턴이 맞지 않는 경우
        dto = new PasswordChangeDto("oldPassword123!", "newpassword123!", "NewPassword456!");
        violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다");
    }
}