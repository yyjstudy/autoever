package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicateEmailException;
import com.autoever.member.exception.DuplicatePhoneNumberException;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateValidationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일 중복 시 DuplicateEmailException 발생")
    void should_ThrowDuplicateEmailException_When_EmailAlreadyExists() {
        // Given
        User existingUser = createTestUser("existing@test.com", "010-1111-1111");
        userRepository.save(existingUser);

        UserRegistrationDto duplicateEmailDto = new UserRegistrationDto(
                "newuser",
                "Password123!",
                "Password123!",
                "새로운 사용자",
                "901010-2345678",
                "existing@test.com", // 중복 이메일
                "010-2222-2222",
                "새 주소"
        );

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(duplicateEmailDto))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다");
    }

    @Test
    @DisplayName("전화번호 중복 시 DuplicatePhoneNumberException 발생")
    void should_ThrowDuplicatePhoneNumberException_When_PhoneNumberAlreadyExists() {
        // Given
        User existingUser = createTestUser("existing@test.com", "010-1111-1111");
        userRepository.save(existingUser);

        UserRegistrationDto duplicatePhoneDto = new UserRegistrationDto(
                "newuser2",
                "Password123!",
                "Password123!",
                "새로운 사용자2",
                "901010-2345679",
                "new@test.com",
                "010-1111-1111", // 중복 전화번호
                "새 주소"
        );

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(duplicatePhoneDto))
                .isInstanceOf(DuplicatePhoneNumberException.class)
                .hasMessageContaining("이미 존재하는 전화번호입니다");
    }

    @Test
    @DisplayName("이메일과 전화번호가 모두 고유할 때 회원가입 성공")
    void should_RegisterSuccessfully_When_EmailAndPhoneNumberAreUnique() {
        // Given
        UserRegistrationDto uniqueDto = new UserRegistrationDto(
                "uniqueuser",
                "Password123!",
                "Password123!", 
                "고유 사용자",
                "901010-2345678",
                "unique@test.com",
                "010-9999-9999",
                "고유 주소"
        );

        // When
        var result = userService.registerUser(uniqueDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("uniqueuser");
        assertThat(result.email()).isEqualTo("unique@test.com");
        assertThat(result.phoneNumber()).isEqualTo("010-****-9999"); // 마스킹된 전화번호
    }

    private User createTestUser(String email, String phoneNumber) {
        return User.builder()
                .username("testuser")
                .password("hashedpassword")
                .name("테스트 사용자")
                .socialNumber("801010-1234567")
                .email(email)
                .phoneNumber(phoneNumber)
                .address("테스트 주소")
                .build();
    }
}