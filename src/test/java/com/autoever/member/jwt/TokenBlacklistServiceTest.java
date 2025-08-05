package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TokenBlacklistService 테스트")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TokenBlacklistServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String EXPIRED_TOKEN = "expired.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        tokenBlacklistService.clearBlacklist();
    }

    @Test
    @DisplayName("유효한 토큰을 블랙리스트에 추가 성공")
    void addToBlacklist_ValidToken_Success() {
        // Given
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        tokenBlacklistService.addToBlacklist(VALID_TOKEN);

        // Then
        assertThat(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).isTrue();
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(1);

        verify(jwtUtil).validateToken(VALID_TOKEN);
        verify(jwtUtil).extractUsername(VALID_TOKEN);
    }

    @Test
    @DisplayName("이미 만료된 토큰은 블랙리스트에 추가하지 않음")
    void addToBlacklist_ExpiredToken_NotAdded() {
        // Given
        when(jwtUtil.validateToken(EXPIRED_TOKEN)).thenReturn(false);

        // When
        tokenBlacklistService.addToBlacklist(EXPIRED_TOKEN);

        // Then
        assertThat(tokenBlacklistService.isBlacklisted(EXPIRED_TOKEN)).isFalse();
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);

        verify(jwtUtil).validateToken(EXPIRED_TOKEN);
        verify(jwtUtil, never()).extractUsername(EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("빈 토큰은 블랙리스트에 추가하지 않음")
    void addToBlacklist_EmptyToken_NotAdded() {
        // When & Then
        tokenBlacklistService.addToBlacklist(null);
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);

        tokenBlacklistService.addToBlacklist("");
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);

        tokenBlacklistService.addToBlacklist("   ");
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 있는지 정확히 확인")
    void isBlacklisted_CheckCorrectly() {
        // Given
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        tokenBlacklistService.addToBlacklist(VALID_TOKEN);

        // When & Then
        assertThat(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted("other.token")).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(null)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted("")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰들을 블랙리스트에서 정리")
    void cleanupExpiredTokens_RemovesExpiredTokens() {
        // Given
        String validToken1 = "valid.token.1";
        String validToken2 = "valid.token.2";
        String expiredToken1 = "expired.token.1";
        String expiredToken2 = "expired.token.2";

        // 유효한 토큰들 추가
        when(jwtUtil.validateToken(validToken1)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken1)).thenReturn("user1");
        when(jwtUtil.validateToken(validToken2)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken2)).thenReturn("user2");
        
        tokenBlacklistService.addToBlacklist(validToken1);
        tokenBlacklistService.addToBlacklist(validToken2);

        // 만료된 토큰들을 블랙리스트에 직접 추가 (테스트를 위해)
        when(jwtUtil.validateToken(expiredToken1)).thenReturn(true);
        when(jwtUtil.extractUsername(expiredToken1)).thenReturn("expiredUser1");
        when(jwtUtil.validateToken(expiredToken2)).thenReturn(true);
        when(jwtUtil.extractUsername(expiredToken2)).thenReturn("expiredUser2");
        
        tokenBlacklistService.addToBlacklist(expiredToken1);
        tokenBlacklistService.addToBlacklist(expiredToken2);

        // cleanup 시에는 만료된 토큰들이 제거되도록 설정
        Date pastDate = new Date(System.currentTimeMillis() - 1000);
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(jwtUtil.extractExpiration(validToken1)).thenReturn(futureDate);
        when(jwtUtil.extractExpiration(validToken2)).thenReturn(futureDate);
        when(jwtUtil.extractExpiration(expiredToken1)).thenReturn(pastDate);
        when(jwtUtil.extractExpiration(expiredToken2)).thenReturn(pastDate);

        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(4);

        // When
        tokenBlacklistService.cleanupExpiredTokens();

        // Then
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(2);
        assertThat(tokenBlacklistService.isBlacklisted(validToken1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(validToken2)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(expiredToken1)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(expiredToken2)).isFalse();
    }

    @Test
    @DisplayName("파싱 실패한 토큰들을 블랙리스트에서 제거")
    void cleanupExpiredTokens_RemovesUnparsableTokens() {
        // Given
        String validToken = "valid.token";
        String unparsableToken = "unparsable.token";

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken)).thenReturn("user");
        when(jwtUtil.validateToken(unparsableToken)).thenReturn(true);
        when(jwtUtil.extractUsername(unparsableToken)).thenReturn("user2");
        
        tokenBlacklistService.addToBlacklist(validToken);
        tokenBlacklistService.addToBlacklist(unparsableToken);

        // cleanup 시 파싱 실패하도록 설정
        when(jwtUtil.extractExpiration(validToken)).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        when(jwtUtil.extractExpiration(unparsableToken)).thenThrow(new RuntimeException("파싱 실패"));

        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(2);

        // When
        tokenBlacklistService.cleanupExpiredTokens();

        // Then
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(1);
        assertThat(tokenBlacklistService.isBlacklisted(validToken)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(unparsableToken)).isFalse();
    }

    @Test
    @DisplayName("사용자의 모든 토큰 블랙리스트 확인")
    void blacklistAllUserTokens_ChecksExistingTokens() {
        // Given
        String userToken1 = "user.token.1";
        String userToken2 = "user.token.2";
        String otherUserToken = "other.user.token";

        when(jwtUtil.validateToken(userToken1)).thenReturn(true);
        when(jwtUtil.extractUsername(userToken1)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(userToken2)).thenReturn(true);
        when(jwtUtil.extractUsername(userToken2)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(otherUserToken)).thenReturn(true);
        when(jwtUtil.extractUsername(otherUserToken)).thenReturn("otheruser");

        tokenBlacklistService.addToBlacklist(userToken1);
        tokenBlacklistService.addToBlacklist(userToken2);
        tokenBlacklistService.addToBlacklist(otherUserToken);

        // When
        tokenBlacklistService.blacklistAllUserTokens(TEST_USERNAME);

        // Then - 이 메서드는 현재 로깅만 하므로 블랙리스트 상태는 변경되지 않음
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("빈 사용자명으로 블랙리스트 요청 시 무시")
    void blacklistAllUserTokens_EmptyUsername_Ignored() {
        // When & Then
        tokenBlacklistService.blacklistAllUserTokens(null);
        tokenBlacklistService.blacklistAllUserTokens("");
        tokenBlacklistService.blacklistAllUserTokens("   ");

        // 아무 에러 없이 정상적으로 처리되어야 함
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("블랙리스트 전체 정리")
    void clearBlacklist_RemovesAllTokens() {
        // Given
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        tokenBlacklistService.addToBlacklist(VALID_TOKEN);
        
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(1);

        // When
        tokenBlacklistService.clearBlacklist();

        // Then
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        assertThat(tokenBlacklistService.isBlacklisted(VALID_TOKEN)).isFalse();
    }

    @Test
    @DisplayName("토큰 블랙리스트 예외 상황 처리")
    void addToBlacklist_ExceptionHandling() {
        // Given
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenThrow(new RuntimeException("토큰 검증 실패"));

        // When
        tokenBlacklistService.addToBlacklist(INVALID_TOKEN);

        // Then - 예외가 발생해도 애플리케이션이 중단되지 않아야 함
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        assertThat(tokenBlacklistService.isBlacklisted(INVALID_TOKEN)).isFalse();
    }
}