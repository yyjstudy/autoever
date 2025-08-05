package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("JWT 고급 기능 통합 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAdvancedFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtProperties jwtProperties;

    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        tokenBlacklistService.clearBlacklist();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 API 접근 후 블랙리스트 추가하면 접근 차단")
    void jwtTokenBlacklist_Integration_Success() throws Exception {
        // Given - 유효한 JWT 토큰 생성
        String validToken = jwtUtil.generateToken(TEST_USERNAME);

        // When - 유효한 토큰으로 API 접근 (성공)
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound()); // 사용자 없어서 404지만 인증은 통과

        // Then - 토큰을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(validToken);
        assertThat(tokenBlacklistService.isBlacklisted(validToken)).isTrue();

        // When - 블랙리스트에 추가된 토큰으로 API 접근 (실패)
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료 시간이 짧은 토큰의 자동 만료 처리")
    void shortExpiryToken_AutoExpiration() throws Exception {
        // Given - 만료 시간이 매우 짧은 토큰 생성 (1ms)
        JwtProperties shortExpiryProperties = new JwtProperties(
            jwtProperties.secretKey(), 
            1L, 
            jwtProperties.tokenPrefix()
        );
        JwtUtil shortExpiryJwtUtil = new JwtUtil(shortExpiryProperties);
        String shortToken = shortExpiryJwtUtil.generateToken(TEST_USERNAME);

        // 토큰이 만료되도록 대기
        Thread.sleep(10);

        // When - 만료된 토큰으로 API 접근
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + shortToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰 처리")
    void malformedJwtToken_HandledProperly() throws Exception {
        // Given - 잘못된 형식의 토큰들
        String[] malformedTokens = {
            "invalid.jwt.token",
            "definitely.not.a.jwt",
            "only.two.parts",
            "",
            "Bearer.without.proper.format"
        };

        // When & Then - 모든 잘못된 토큰들이 올바르게 거부되어야 함
        for (String token : malformedTokens) {
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("블랙리스트 서비스 기능 종합 테스트")
    void tokenBlacklistService_ComprehensiveTest() {
        // Given
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");
        String token3 = jwtUtil.generateToken("user3");

        // When - 토큰들을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(token1);
        tokenBlacklistService.addToBlacklist(token2);
        tokenBlacklistService.addToBlacklist(token3);

        // Then - 블랙리스트 상태 확인
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(3);
        assertThat(tokenBlacklistService.isBlacklisted(token1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(token2)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(token3)).isTrue();

        // When - 블랙리스트 정리
        tokenBlacklistService.cleanupExpiredTokens();

        // Then - 유효한 토큰들은 여전히 블랙리스트에 있어야 함
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(3);

        // When - 전체 정리
        tokenBlacklistService.clearBlacklist();

        // Then - 모든 토큰이 제거되어야 함
        assertThat(tokenBlacklistService.getBlacklistSize()).isEqualTo(0);
        assertThat(tokenBlacklistService.isBlacklisted(token1)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(token2)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(token3)).isFalse();
    }

    @Test
    @DisplayName("JWT 필터와 블랙리스트 서비스 연동 테스트")
    void jwtFilterAndBlacklistService_Integration() throws Exception {
        // Given
        String token = jwtUtil.generateToken(TEST_USERNAME);

        // When - 처음에는 토큰이 유효해서 인증 통과
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // 인증은 통과, 사용자 없어서 404

        // Then - 토큰을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(token);

        // When - 블랙리스트에 추가된 후에는 인증 실패
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다양한 Authorization 헤더 형식 처리")
    void variousAuthorizationHeaderFormats_HandledCorrectly() throws Exception {
        String validToken = jwtUtil.generateToken(TEST_USERNAME);

        // Given - 다양한 헤더 형식들
        String[] validHeaders = {
            "Bearer " + validToken
        };

        String[] invalidHeaders = {
            "Basic " + validToken,
            "bearer " + validToken,  // 소문자
            validToken,              // Bearer 없음
            "Bearer",                // 토큰 없음
            "Bearer ",               // 빈 토큰
            ""                       // 빈 헤더
        };

        // When & Then - 유효한 헤더 형식들은 인증 통과
        for (String header : validHeaders) {
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", header))
                    .andExpect(status().isNotFound()); // 인증은 통과, 사용자 없어서 404
        }

        // When & Then - 유효하지 않은 헤더 형식들은 인증 실패
        for (String header : invalidHeaders) {
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", header))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("토큰 없는 요청과 잘못된 토큰 요청 구분")
    void noTokenVsInvalidToken_DistinguishedCorrectly() throws Exception {
        // When & Then - 토큰 없는 요청
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        // When & Then - 잘못된 토큰 요청
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());

        // When & Then - 빈 토큰 요청
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("블랙리스트된 토큰과 만료된 토큰의 다른 처리")
    void blacklistedVsExpiredToken_DifferentHandling() throws Exception {
        // Given - 유효한 토큰과 만료 시간이 짧은 토큰
        String validToken = jwtUtil.generateToken(TEST_USERNAME);
        
        JwtProperties shortExpiryProperties = new JwtProperties(
            jwtProperties.secretKey(), 
            1L, 
            jwtProperties.tokenPrefix()
        );
        JwtUtil shortExpiryJwtUtil = new JwtUtil(shortExpiryProperties);
        String expiredToken = shortExpiryJwtUtil.generateToken(TEST_USERNAME);
        
        // 토큰이 만료되도록 대기
        Thread.sleep(10);

        // When - 유효한 토큰을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(validToken);

        // Then - 블랙리스트된 토큰과 만료된 토큰 모두 접근 거부
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }
}