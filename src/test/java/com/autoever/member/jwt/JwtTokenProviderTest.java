package com.autoever.member.jwt;

import com.autoever.member.dto.JwtTokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * JwtTokenProvider 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    @Mock
    private JwtService jwtService;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    private static final long TEST_EXPIRES_IN = 3600000L; // 1시간

    // Mock 설정은 각 테스트에서 개별적으로 수행

    @Test
    @DisplayName("Authentication 객체로부터 JWT 토큰 생성 성공")
    void generateToken_WithAuthentication_ShouldReturnJwtTokenDto() {
        // Given
        given(authentication.getName()).willReturn(TEST_USERNAME);
        given(jwtService.generateToken(TEST_USERNAME)).willReturn(TEST_TOKEN);
        given(jwtService.getExpirationTime()).willReturn(TEST_EXPIRES_IN);
        
        // When
        JwtTokenDto result = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(TEST_EXPIRES_IN / 1000); // 밀리초를 초로 변환
    }

    @Test
    @DisplayName("사용자명으로 직접 JWT 토큰 생성 성공")
    void generateTokenForUsername_WithUsername_ShouldReturnJwtTokenDto() {
        // Given
        given(jwtService.generateToken(TEST_USERNAME)).willReturn(TEST_TOKEN);
        given(jwtService.getExpirationTime()).willReturn(TEST_EXPIRES_IN);
        
        // When
        JwtTokenDto result = jwtTokenProvider.generateTokenForUsername(TEST_USERNAME);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(TEST_EXPIRES_IN / 1000);
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 유효한 토큰")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        given(jwtService.validateToken(TEST_TOKEN)).willReturn(true);

        // When
        boolean result = jwtTokenProvider.validateToken(TEST_TOKEN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 무효한 토큰")
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";
        given(jwtService.validateToken(invalidToken)).willReturn(false);

        // When
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자명 추출 성공")
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        // Given
        given(jwtService.extractUsername(TEST_TOKEN)).willReturn(TEST_USERNAME);

        // When
        String result = jwtTokenProvider.extractUsername(TEST_TOKEN);

        // Then
        assertThat(result).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("토큰 만료 시간 조회 성공")
    void getTokenExpirationTime_ShouldReturnExpirationTime() {
        // Given
        given(jwtService.getExpirationTime()).willReturn(TEST_EXPIRES_IN);
        
        // When
        long result = jwtTokenProvider.getTokenExpirationTime();

        // Then
        assertThat(result).isEqualTo(TEST_EXPIRES_IN);
    }

    @Test
    @DisplayName("JwtTokenDto 생성 시 올바른 형식으로 변환")
    void generateToken_ShouldCreateCorrectJwtTokenDtoFormat() {
        // Given
        long longExpirationTime = 7200000L; // 2시간
        given(authentication.getName()).willReturn(TEST_USERNAME);
        given(jwtService.generateToken(TEST_USERNAME)).willReturn(TEST_TOKEN);
        given(jwtService.getExpirationTime()).willReturn(longExpirationTime);

        // When
        JwtTokenDto result = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(result.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(7200); // 밀리초를 초로 변환 확인
    }

    @Test
    @DisplayName("Authentication 객체에서 사용자명이 null인 경우")
    void generateToken_WithNullUsername_ShouldHandleGracefully() {
        // Given
        given(authentication.getName()).willReturn(null);
        given(jwtService.generateToken(null)).willReturn(TEST_TOKEN);

        // When
        JwtTokenDto result = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("빈 문자열 사용자명으로 토큰 생성")
    void generateTokenForUsername_WithEmptyUsername_ShouldHandleGracefully() {
        // Given
        String emptyUsername = "";
        given(jwtService.generateToken(emptyUsername)).willReturn(TEST_TOKEN);

        // When
        JwtTokenDto result = jwtTokenProvider.generateTokenForUsername(emptyUsername);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }
}