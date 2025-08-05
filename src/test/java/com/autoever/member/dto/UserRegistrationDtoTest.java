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

@DisplayName("UserRegistrationDto 유효성 검증 테스트")
class UserRegistrationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("모든 필드가 유효한 경우 검증 통과")
    void validUserRegistrationDto() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("사용자명이 null인 경우 검증 실패")
    void usernameNull() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            null,
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("사용자명은 필수입니다");
    }

    @Test
    @DisplayName("사용자명이 너무 짧은 경우 검증 실패")
    void usernameTooShort() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "ab",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("사용자명은 3-50자 사이여야 합니다");
    }

    @Test
    @DisplayName("사용자명에 특수문자가 포함된 경우 검증 실패")
    void usernameInvalidCharacters() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "test@user",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("사용자명은 영문, 숫자, 밑줄(_)만 사용 가능합니다");
    }

    @Test
    @DisplayName("비밀번호가 조건을 만족하지 않는 경우 검증 실패")
    void passwordInvalid() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "password",
            "password",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다");
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 일치하지 않는 경우 검증 실패")
    void passwordsDoNotMatch() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password456!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호와 비밀번호 확인이 일치하지 않습니다");
    }

    @Test
    @DisplayName("이름에 숫자가 포함된 경우 검증 실패")
    void nameInvalidCharacters() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동123",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("이름은 한글, 영문, 공백만 사용 가능합니다");
    }

    @Test
    @DisplayName("주민등록번호 형식이 잘못된 경우 검증 실패")
    void socialNumberInvalidFormat() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "9012011234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("주민등록번호 형식이 올바르지 않습니다 (XXXXXX-XXXXXXX)");
    }

    @Test
    @DisplayName("이메일 형식이 잘못된 경우 검증 실패")
    void emailInvalidFormat() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "invalid-email",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("올바른 이메일 형식이 아닙니다");
    }

    @Test
    @DisplayName("전화번호 형식이 잘못된 경우 검증 실패")
    void phoneNumberInvalidFormat() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "01012345678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("전화번호 형식이 올바르지 않습니다 (XXX-XXXX-XXXX)");
    }

    @Test
    @DisplayName("주소가 너무 짧은 경우 검증 실패")
    void addressTooShort() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("주소는 5-500자 사이여야 합니다");
    }

    @Test
    @DisplayName("여러 필드가 동시에 유효하지 않은 경우")
    void multipleFieldsInvalid() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "ab",
            "weak",
            "different",
            "",
            "invalid-social",
            "invalid-email",
            "invalid-phone",
            ""
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSizeGreaterThan(5);
    }

    @Test
    @DisplayName("영문 이름도 유효한 경우")
    void englishNameValid() {
        // given
        UserRegistrationDto dto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "John Doe",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when
        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }
}