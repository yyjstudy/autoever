package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService 테스트")
@ActiveProfiles("test")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    private static final String SECRET_KEY = "YXV0b2V2ZXJTZWNyZXRLZXlGb3JKV1RUb2tlbkdlbmVyYXRpb25BbmRWYWxpZGF0aW9uMjAyNCE=";
    private static final long EXPIRATION_TIME = 3600000; // 1 hour
    private static final String TOKEN_PREFIX = "Bearer";
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(SECRET_KEY, EXPIRATION_TIME, TOKEN_PREFIX);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    @DisplayName("JWT 토큰 생성 성공")
    void generateToken_Success() {
        // When
        String token = jwtService.generateToken(TEST_USERNAME);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 부분으로 구성
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자명 추출 성공")
    void extractUsername_Success() {
        // Given
        String token = jwtService.generateToken(TEST_USERNAME);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("JWT 토큰에서 만료 시간 추출 성공")
    void extractExpiration_Success() {
        // Given
        Date beforeGeneration = new Date();
        String token = jwtService.generateToken(TEST_USERNAME);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration.getTime() - beforeGeneration.getTime())
                .isGreaterThanOrEqualTo(EXPIRATION_TIME - 1000) // 1초 여유
                .isLessThanOrEqualTo(EXPIRATION_TIME + 1000);
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 성공")
    void validateToken_WithUsername_Success() {
        // Given
        String token = jwtService.generateToken(TEST_USERNAME);

        // When
        Boolean isValid = jwtService.validateToken(token, TEST_USERNAME);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 사용자명으로 JWT 토큰 검증 실패")
    void validateToken_WithWrongUsername_Failure() {
        // Given
        String token = jwtService.generateToken(TEST_USERNAME);

        // When
        Boolean isValid = jwtService.validateToken(token, "wronguser");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("사용자명 없이 JWT 토큰 검증 성공")
    void validateToken_WithoutUsername_Success() {
        // Given
        String token = jwtService.generateToken(TEST_USERNAME);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰 검증 실패")
    void validateToken_InvalidFormat_Failure() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Authorization 헤더에서 토큰 추출 성공")
    void extractTokenFromHeader_Success() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        String authHeader = "Bearer " + token;

        // When
        String extractedToken = jwtService.extractTokenFromHeader(authHeader);

        // Then
        assertThat(extractedToken).isEqualTo(token);
    }

    @Test
    @DisplayName("잘못된 Authorization 헤더에서 토큰 추출 실패")
    void extractTokenFromHeader_InvalidHeader_ReturnsNull() {
        // Given
        String authHeader = "Basic dXNlcjpwYXNzd29yZA==";

        // When
        String extractedToken = jwtService.extractTokenFromHeader(authHeader);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    @DisplayName("null Authorization 헤더에서 토큰 추출 시 null 반환")
    void extractTokenFromHeader_NullHeader_ReturnsNull() {
        // When
        String extractedToken = jwtService.extractTokenFromHeader(null);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    @DisplayName("JWT 토큰 생성 및 파싱 검증")
    void generateAndParseToken_Success() {
        // Given
        String username = "testuser123";
        
        // When
        String token = jwtService.generateToken(username);
        
        // Then - 토큰을 직접 파싱하여 검증
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(EXPIRATION_TIME);
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 실패")
    void validateToken_Expired_Failure() {
        // Given - 만료 시간을 1밀리초로 설정
        JwtProperties shortExpiryProperties = new JwtProperties(SECRET_KEY, 1L, TOKEN_PREFIX);
        JwtService shortExpiryJwtService = new JwtService(shortExpiryProperties);
        
        String token = shortExpiryJwtService.generateToken(TEST_USERNAME);
        
        // 토큰이 만료되도록 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        Boolean isValid = shortExpiryJwtService.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }
}