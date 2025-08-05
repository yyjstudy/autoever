package com.autoever.member.dto;

import com.autoever.member.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserResponseDtoTest {

    @Test
    @DisplayName("User 엔티티로부터 UserResponseDto 생성 테스트")
    void createUserResponseDtoFromUserTest() {
        // given
        User user = User.builder()
                .username("testuser")
                .password("password123!")
                .name("홍길동")
                .socialNumber("901201-1234567")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구 테헤란로 123")
                .build();

        // when
        UserResponseDto responseDto = UserResponseDto.from(user);

        // then
        assertThat(responseDto.username()).isEqualTo("testuser");
        assertThat(responseDto.name()).isEqualTo("홍길동");
        assertThat(responseDto.socialNumber()).isEqualTo("901201-*******"); // 마스킹된 주민번호
        assertThat(responseDto.phoneNumber()).isEqualTo("010-****-5678"); // 마스킹된 전화번호
        assertThat(responseDto.address()).isEqualTo("서울시 강남구 테헤란로 123");
    }

    @Test
    @DisplayName("UserResponseDto.from() 정적 메서드 테스트")
    void createUserResponseDtoUsingFromMethodTest() {
        // given
        User user = User.builder()
                .username("testuser2")
                .password("password123!")
                .name("김철수")
                .socialNumber("890101-1234567")
                .phoneNumber("010-9876-5432")
                .address("부산시 해운대구")
                .build();

        // when
        UserResponseDto responseDto = UserResponseDto.from(user);

        // then
        assertThat(responseDto.username()).isEqualTo("testuser2");
        assertThat(responseDto.name()).isEqualTo("김철수");
        assertThat(responseDto.socialNumber()).isEqualTo("890101-*******");
        assertThat(responseDto.phoneNumber()).isEqualTo("010-****-5432");
        assertThat(responseDto.address()).isEqualTo("부산시 해운대구");
    }

    @Test
    @DisplayName("민감한 정보가 마스킹되어 노출되지 않는지 테스트")
    void maskSensitiveInformationTest() {
        // given
        User user = User.builder()
                .username("sensitiveUser")
                .password("verySensitivePassword123!")
                .name("테스트사용자")
                .socialNumber("951010-1234567")
                .phoneNumber("010-5555-1234")
                .address("대전시 유성구")
                .build();

        // when
        UserResponseDto responseDto = UserResponseDto.from(user);

        // then
        // 비밀번호는 ResponseDto에 포함되지 않음
        assertThat(responseDto.toString()).doesNotContain("verySensitivePassword123!");
        
        // 주민번호와 전화번호는 마스킹됨
        assertThat(responseDto.socialNumber()).isNotEqualTo("951010-1234567");
        assertThat(responseDto.phoneNumber()).isNotEqualTo("010-5555-1234");
        
        // 마스킹된 형태로 포함됨
        assertThat(responseDto.socialNumber()).isEqualTo("951010-*******");
        assertThat(responseDto.phoneNumber()).isEqualTo("010-****-1234");
    }
}