package com.autoever.member.jwt;

import com.autoever.member.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter 테스트")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String TEST_USERNAME = "testuser";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 성공")
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader(JwtProperties.HEADER_STRING)).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractTokenFromHeader(BEARER_TOKEN)).thenReturn(VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(TEST_USERNAME);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 인증 실패")
    void doFilterInternal_InvalidToken_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader(JwtProperties.HEADER_STRING)).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtUtil.extractTokenFromHeader("Bearer " + INVALID_TOKEN)).thenReturn(INVALID_TOKEN);
        when(jwtUtil.validateToken(INVALID_TOKEN)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없는 경우")
    void doFilterInternal_NoAuthorizationHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader(JwtProperties.HEADER_STRING)).thenReturn(null);
        when(jwtUtil.extractTokenFromHeader(null)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(any());
        verify(jwtUtil, never()).extractUsername(any());
    }

    @Test
    @DisplayName("잘못된 형식의 Authorization 헤더")
    void doFilterInternal_MalformedAuthorizationHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        String malformedHeader = "Basic dXNlcjpwYXNzd29yZA==";
        when(request.getHeader(JwtProperties.HEADER_STRING)).thenReturn(malformedHeader);
        when(jwtUtil.extractTokenFromHeader(malformedHeader)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(any());
    }

    @Test
    @DisplayName("JWT 처리 중 예외 발생 시 SecurityContext 클리어")
    void doFilterInternal_ExceptionThrown_ClearsSecurityContext() throws ServletException, IOException {
        // Given
        when(request.getHeader(JwtProperties.HEADER_STRING)).thenReturn(BEARER_TOKEN);
        when(jwtUtil.extractTokenFromHeader(BEARER_TOKEN)).thenThrow(new RuntimeException("JWT 처리 오류"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("회원가입 경로는 필터 적용 제외")
    void shouldNotFilter_RegisterPath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/api/users/register");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("로그인 경로는 필터 적용 제외")
    void shouldNotFilter_LoginPath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/api/users/login");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Swagger UI 경로는 필터 적용 제외")
    void shouldNotFilter_SwaggerPath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/swagger-ui/index.html");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("API 문서 경로는 필터 적용 제외")
    void shouldNotFilter_ApiDocsPath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/v3/api-docs/swagger-config");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("H2 콘솔 경로는 필터 적용 제외")
    void shouldNotFilter_H2ConsolePath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/h2-console/login.do");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("루트 경로는 필터 적용 제외")
    void shouldNotFilter_RootPath_ReturnsTrue() {
        // Given
        when(request.getServletPath()).thenReturn("/");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("보호된 API 경로는 필터 적용")
    void shouldNotFilter_ProtectedApiPath_ReturnsFalse() {
        // Given
        when(request.getServletPath()).thenReturn("/api/users/me");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("관리자 API 경로는 필터 적용")
    void shouldNotFilter_AdminApiPath_ReturnsFalse() {
        // Given
        when(request.getServletPath()).thenReturn("/api/admin/users");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isFalse();
    }
}