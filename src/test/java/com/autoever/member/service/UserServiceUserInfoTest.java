package com.autoever.member.service;

import com.autoever.member.dto.UserInfoDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.UserNotFoundException;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * UserService 본인 정보 조회 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - 본인 정보 조회 테스트")
class UserServiceUserInfoTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final String TEST_USERNAME = "testuser123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username(TEST_USERNAME)
            .password("hashedPassword")
            .name("홍길동")
            .socialNumber("901201-1234567")
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address("서울특별시 강남구 테헤란로 123")
            .build();
    }

    @Nested
    @DisplayName("getCurrentUserInfo 메서드 테스트")
    class GetCurrentUserInfoTest {

        @Test
        @DisplayName("사용자 정보 조회 성공 - 민감정보 마스킹 적용")
        void getCurrentUserInfo_Success() {
            // Given
            when(userRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.of(testUser));

            // When
            UserInfoDto result = userService.getCurrentUserInfo(TEST_USERNAME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isNull(); // Mock 객체는 ID가 없음
            assertThat(result.username()).isEqualTo(TEST_USERNAME);
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.socialNumber()).isEqualTo("901201-1******"); // 마스킹 확인
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
            assertThat(result.address()).isEqualTo("서울특별시"); // 주소 필터링 확인
            assertThat(result.createdAt()).isNull(); // Mock 객체는 타임스탬프가 없음
            assertThat(result.updatedAt()).isNull(); // Mock 객체는 타임스탬프가 없음
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void getCurrentUserInfo_UserNotFound() {
            // Given
            String nonExistentUsername = "nonexistent";
            when(userRepository.findByUsername(nonExistentUsername))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getCurrentUserInfo(nonExistentUsername))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다")
                .hasMessageContaining(nonExistentUsername);
        }

        @Test
        @DisplayName("다양한 주소 패턴에 대한 필터링 테스트")
        void getCurrentUserInfo_AddressFiltering() {
            // Given - 다양한 주소 패턴 테스트
            User userWithDifferentAddress = User.builder()
                .username("addresstest")
                .password("hashedPassword")
                .name("테스트사용자")
                .socialNumber("850315-2345678")
                .email("addresstest@example.com")
                .phoneNumber("010-9876-5432")
                .address("부산광역시 해운대구 우동 1234")
                .build();

            when(userRepository.findByUsername("addresstest"))
                .thenReturn(Optional.of(userWithDifferentAddress));

            // When
            UserInfoDto result = userService.getCurrentUserInfo("addresstest");

            // Then
            assertThat(result.address()).isEqualTo("부산광역시");
        }

        @Test
        @DisplayName("특별시 주소 필터링 테스트")
        void getCurrentUserInfo_SpecialCityFiltering() {
            // Given
            User userWithSpecialCity = User.builder()
                .username("specialcity")
                .password("hashedPassword")
                .name("특별시사용자")
                .socialNumber("751123-1111111")
                .email("special@example.com")
                .phoneNumber("010-5555-5555")
                .address("인천광역시 연수구 송도동")
                .build();

            when(userRepository.findByUsername("specialcity"))
                .thenReturn(Optional.of(userWithSpecialCity));

            // When
            UserInfoDto result = userService.getCurrentUserInfo("specialcity");

            // Then
            assertThat(result.address()).isEqualTo("인천광역시");
        }

        @Test
        @DisplayName("주민등록번호 마스킹 패턴 확인")
        void getCurrentUserInfo_SocialNumberMasking() {
            // Given - 다른 패턴의 주민등록번호
            User userWithDifferentSocialNumber = User.builder()
                .username("socialnumbertest")
                .password("hashedPassword")
                .name("주민번호테스트")
                .socialNumber("820607-2987654")
                .email("social@example.com")
                .phoneNumber("010-7777-7777")
                .address("대구광역시 달서구")
                .build();

            when(userRepository.findByUsername("socialnumbertest"))
                .thenReturn(Optional.of(userWithDifferentSocialNumber));

            // When
            UserInfoDto result = userService.getCurrentUserInfo("socialnumbertest");

            // Then
            assertThat(result.socialNumber()).isEqualTo("820607-2******");
        }

        @Test
        @DisplayName("빈 주소에 대한 안전한 처리")
        void getCurrentUserInfo_EmptyAddressSafeHandling() {
            // Given
            User userWithEmptyAddress = User.builder()
                .username("emptyaddress")
                .password("hashedPassword")
                .name("빈주소사용자")
                .socialNumber("901201-1234567")
                .email("empty@example.com")
                .phoneNumber("010-1111-1111")
                .address("")
                .build();

            when(userRepository.findByUsername("emptyaddress"))
                .thenReturn(Optional.of(userWithEmptyAddress));

            // When
            UserInfoDto result = userService.getCurrentUserInfo("emptyaddress");

            // Then
            assertThat(result.address()).isEmpty(); // 빈 문자열이 그대로 반환되어야 함
        }

        @Test
        @DisplayName("null 주소에 대한 안전한 처리")
        void getCurrentUserInfo_NullAddressSafeHandling() {
            // Given
            User userWithNullAddress = User.builder()
                .username("nulladdress")
                .password("hashedPassword")
                .name("널주소사용자")
                .socialNumber("901201-1234567")
                .email("null@example.com")
                .phoneNumber("010-2222-2222")
                .address(null)
                .build();

            when(userRepository.findByUsername("nulladdress"))
                .thenReturn(Optional.of(userWithNullAddress));

            // When
            UserInfoDto result = userService.getCurrentUserInfo("nulladdress");

            // Then
            assertThat(result.address()).isNull(); // null이 그대로 반환되어야 함
        }
    }
}