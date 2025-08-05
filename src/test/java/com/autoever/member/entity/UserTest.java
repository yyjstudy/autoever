package com.autoever.member.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("User 엔티티 생성 테스트")
    void createUserTest() {
        // given
        String username = "testuser";
        String password = "testPassword123!";
        String name = "홍길동";
        String socialNumber = "901201-1234567";
        String phoneNumber = "010-1234-5678";
        String address = "서울시 강남구 테헤란로 123";

        // when
        User user = User.builder()
                .username(username)
                .password(password)
                .name(name)
                .socialNumber(socialNumber)
                .phoneNumber(phoneNumber)
                .address(address)
                .build();

        // then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getSocialNumber()).isEqualTo(socialNumber);
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(user.getAddress()).isEqualTo(address);
    }

    @Test
    @DisplayName("비밀번호 업데이트 테스트")
    void updatePasswordTest() {
        // given
        User user = createTestUser();
        String newPassword = "newPassword456!";

        // when
        user.updatePassword(newPassword);

        // then
        assertThat(user.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("프로필 업데이트 테스트")
    void updateProfileTest() {
        // given
        User user = createTestUser();
        String newName = "김철수";
        String newPhoneNumber = "010-9876-5432";
        String newAddress = "부산시 해운대구 센텀로 456";

        // when
        user.updateProfile(newName, newPhoneNumber, newAddress);

        // then
        assertThat(user.getName()).isEqualTo(newName);
        assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
        assertThat(user.getAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("주민번호 마스킹 테스트")
    void getMaskedSocialNumberTest() {
        // given
        User user = createTestUser();

        // when
        String maskedSocialNumber = user.getMaskedSocialNumber();

        // then
        assertThat(maskedSocialNumber).isEqualTo("901201-*******");
    }

    @Test
    @DisplayName("전화번호 마스킹 테스트")
    void getMaskedPhoneNumberTest() {
        // given
        User user = createTestUser();

        // when
        String maskedPhoneNumber = user.getMaskedPhoneNumber();

        // then
        assertThat(maskedPhoneNumber).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("주민번호 마스킹 - null 값 테스트")
    void getMaskedSocialNumberNullTest() {
        // given
        User user = User.builder()
                .username("test")
                .password("password")
                .name("test")
                .socialNumber(null)
                .phoneNumber("010-1234-5678")
                .address("address")
                .build();

        // when
        String maskedSocialNumber = user.getMaskedSocialNumber();

        // then
        assertThat(maskedSocialNumber).isEqualTo("***-***");
    }

    @Test
    @DisplayName("전화번호 마스킹 - null 값 테스트")
    void getMaskedPhoneNumberNullTest() {
        // given
        User user = User.builder()
                .username("test")
                .password("password")
                .name("test")
                .socialNumber("901201-1234567")
                .phoneNumber(null)
                .address("address")
                .build();

        // when
        String maskedPhoneNumber = user.getMaskedPhoneNumber();

        // then
        assertThat(maskedPhoneNumber).isEqualTo("***-****-****");
    }

    @Test
    @DisplayName("toString 메서드에서 민감한 정보 제외 테스트")
    void toStringExcludeSensitiveDataTest() {
        // given
        User user = createTestUser();

        // when
        String userString = user.toString();

        // then
        assertThat(userString).doesNotContain("testPassword123!");
        assertThat(userString).doesNotContain("901201-1234567");
        assertThat(userString).contains("testuser");
        assertThat(userString).contains("홍길동");
    }

    private User createTestUser() {
        return User.builder()
                .username("testuser")
                .password("testPassword123!")
                .name("홍길동")
                .socialNumber("901201-1234567")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구 테헤란로 123")
                .build();
    }
}