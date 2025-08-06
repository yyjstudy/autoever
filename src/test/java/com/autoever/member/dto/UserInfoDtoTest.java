package com.autoever.member.dto;

import com.autoever.member.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * UserInfoDto 테스트
 * 민감정보 마스킹 및 주소 필터링 로직 검증
 */
@DisplayName("UserInfoDto 테스트")
class UserInfoDtoTest {

    @Nested
    @DisplayName("from 메서드 - User 엔티티로부터 DTO 생성")
    class FromMethodTest {

        @Test
        @DisplayName("정상적인 User 엔티티로부터 UserInfoDto 생성 성공")
        void from_Success() {
            // Given
            User user = User.builder()
                .username("testuser123")
                .password("hashedPassword")
                .name("홍길동")
                .socialNumber("901201-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울특별시 강남구 테헤란로 123")
                .build();

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.id()).isNull(); // ID는 생성 시점에 null
            assertThat(result.username()).isEqualTo("testuser123");
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.socialNumber()).isEqualTo("901201-1******");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
            assertThat(result.address()).isEqualTo("서울특별시");
            assertThat(result.createdAt()).isNull(); // 생성 시점에 null
            assertThat(result.updatedAt()).isNull(); // 생성 시점에 null
        }
    }

    @Nested
    @DisplayName("주민등록번호 마스킹 테스트")
    class SocialNumberMaskingTest {

        @ParameterizedTest
        @ValueSource(strings = {
            "901201-1234567", // 일반적인 패턴
            "850315-2987654", // 다른 패턴
            "751123-1111111", // 또 다른 패턴
            "820607-2000000"  // 0이 포함된 패턴
        })
        @DisplayName("정상적인 주민등록번호 마스킹 (뒤 6자리 ******)")
        void maskSocialNumber_ValidFormats(String socialNumber) {
            // Given
            User user = createTestUser(socialNumber, "서울특별시 강남구");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            String expectedMasked = socialNumber.substring(0, 8) + "******";
            assertThat(result.socialNumber()).isEqualTo(expectedMasked);
        }

        @Test
        @DisplayName("짧은 주민등록번호는 마스킹하지 않고 원본 반환")
        void maskSocialNumber_ShortFormat() {
            // Given
            User user = createTestUser("901201-1", "서울특별시");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.socialNumber()).isEqualTo("901201-1"); // 원본 그대로
        }

        @Test
        @DisplayName("null 주민등록번호는 null 반환")
        void maskSocialNumber_Null() {
            // Given
            User user = createTestUser(null, "서울특별시");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.socialNumber()).isNull();
        }

        @Test
        @DisplayName("빈 문자열 주민등록번호는 빈 문자열 반환")
        void maskSocialNumber_Empty() {
            // Given
            User user = createTestUser("", "서울특별시");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.socialNumber()).isEmpty();
        }
    }

    @Nested
    @DisplayName("주소 필터링 테스트")
    class AddressFilteringTest {

        @ParameterizedTest
        @ValueSource(strings = {
            "서울특별시 강남구 테헤란로 123", // 특별시
            "부산광역시 해운대구 우동 1234", // 광역시
            "인천광역시 연수구 송도동",       // 광역시
            "대구광역시 달서구",             // 광역시
            "대전광역시 서구 둔산동"         // 광역시
        })
        @DisplayName("시/도 레벨로 주소 필터링 - 광역시, 특별시")
        void filterAddress_CityLevel(String fullAddress) {
            // Given
            User user = createTestUser("901201-1234567", fullAddress);

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            String expectedTopLevel = fullAddress.split("\\s+")[0];
            assertThat(result.address()).isEqualTo(expectedTopLevel);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "경기도 성남시 분당구",
            "강원도 춘천시",
            "충청남도 천안시 동남구",
            "전라북도 전주시",
            "경상남도 창원시 마산회원구"
        })
        @DisplayName("시/도 레벨로 주소 필터링 - 일반 도")
        void filterAddress_ProvinceLevel(String fullAddress) {
            // Given
            User user = createTestUser("901201-1234567", fullAddress);

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            String expectedTopLevel = fullAddress.split("\\s+")[0];
            assertThat(result.address()).isEqualTo(expectedTopLevel);
        }

        @Test
        @DisplayName("세종특별자치시 주소 필터링")
        void filterAddress_SejongCity() {
            // Given
            User user = createTestUser("901201-1234567", "세종특별자치시 조치원읍");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isEqualTo("세종특별자치시");
        }

        @Test
        @DisplayName("제주특별자치도 주소 필터링")
        void filterAddress_JejuProvince() {
            // Given
            User user = createTestUser("901201-1234567", "제주특별자치도 제주시");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isEqualTo("제주특별자치도");
        }

        @Test
        @DisplayName("null 주소는 null 반환")
        void filterAddress_Null() {
            // Given
            User user = createTestUser("901201-1234567", null);

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isNull();
        }

        @Test
        @DisplayName("빈 문자열 주소는 빈 문자열 반환")
        void filterAddress_Empty() {
            // Given
            User user = createTestUser("901201-1234567", "");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isEmpty();
        }

        @Test
        @DisplayName("공백만 있는 주소는 공백 문자열 반환")
        void filterAddress_WhitespaceOnly() {
            // Given
            User user = createTestUser("901201-1234567", "   ");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isEqualTo("   ");
        }

        @Test
        @DisplayName("비정형 주소는 안전하게 처리 (길이 제한)")
        void filterAddress_UnconventionalFormat() {
            // Given - 매우 긴 주소
            String longAddress = "이것은 매우 긴 주소입니다 정말로 매우 긴 주소이고 20자를 넘어갑니다";
            User user = createTestUser("901201-1234567", longAddress);

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).hasSize(23); // 20자 + "..." = 23자
            assertThat(result.address()).endsWith("...");
        }

        @Test
        @DisplayName("단일 단어 주소는 그대로 반환")
        void filterAddress_SingleWord() {
            // Given
            User user = createTestUser("901201-1234567", "서울");

            // When
            UserInfoDto result = UserInfoDto.from(user);

            // Then
            assertThat(result.address()).isEqualTo("서울");
        }
    }

    /**
     * 테스트용 User 객체 생성 헬퍼 메서드
     */
    private User createTestUser(String socialNumber, String address) {
        User user = User.builder()
            .username("testuser")
            .password("password")
            .name("테스트사용자")
            .socialNumber(socialNumber)
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address(address)
            .build();
        return user;
    }
}