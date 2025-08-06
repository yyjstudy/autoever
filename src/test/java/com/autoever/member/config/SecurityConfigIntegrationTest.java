package com.autoever.member.config;

import com.autoever.member.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SecurityConfig 통합 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "async.enabled=false")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("회원가입 API는 인증 없이 접근 가능")
    void registerEndpoint_NoAuth_Accessible() throws Exception {
        mockMvc.perform(post("/api/users/register"))
                .andExpect(status().isBadRequest()); // 데이터 없어서 400이지만 인증은 통과
    }

    @Test
    @DisplayName("로그인 API는 인증 없이 접근 가능")
    void loginEndpoint_NoAuth_Accessible() throws Exception {
        String loginJson = "{\"username\":\"testuser\",\"password\":\"Test1234!\"}";
        
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized()); // 사용자가 없어서 401이지만 인증 필터는 통과
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 API 접근 성공")
    void protectedEndpoint_ValidJWT_Success() throws Exception {
        // Given
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // When & Then
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // 사용자 없어서 404지만 인증은 통과
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 보호된 API 접근 거부")
    void protectedEndpoint_InvalidJWT_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 토큰 없이 보호된 API 접근 거부")
    void protectedEndpoint_NoJWT_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Basic Auth로 관리자 API 접근 성공")
    void adminEndpoint_BasicAuth_Success() throws Exception {
        // Given
        String credentials = Base64.getEncoder().encodeToString("admin:1212".getBytes());

        // When & Then
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 Basic Auth로 관리자 API 접근 거부")
    void adminEndpoint_InvalidBasicAuth_Unauthorized() throws Exception {
        // Given
        String credentials = Base64.getEncoder().encodeToString("admin:wrong".getBytes());

        // When & Then
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Basic " + credentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 역할의 JWT 토큰으로 관리자 API 접근 - 403 (역할 부족)")
    void adminEndpoint_JWTWithoutAdminRole_Forbidden() throws Exception {
        // Given - JWT는 기본적으로 ROLE_USER 권한만 부여
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // When & Then
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()); // JWT는 ROLE_USER이므로 ADMIN 접근 불가
    }

    @Test
    @DisplayName("Swagger UI는 인증 필요")
    void swaggerUI_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 Swagger UI 접근 가능")
    @WithMockUser
    void swaggerUI_WithAuth_Success() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk()); // Spring Boot가 정적 리소스로 처리해서 실제로는 리다이렉트됨
    }

    @Test
    @DisplayName("H2 콘솔은 인증 필요")
    void h2Console_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 필터가 예외 처리를 올바르게 수행")
    void jwtFilter_ExceptionHandling_ContinuesFilterChain() throws Exception {
        // Given - 잘못된 형식의 Authorization 헤더
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer 토큰 형식이지만 토큰이 비어있는 경우")
    void jwtFilter_EmptyBearerToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("복수 인증 헤더가 있는 경우 JWT 우선 처리")
    void multipleAuthHeaders_JWTPriority() throws Exception {
        // Given
        String username = "testuser";
        String validJWT = jwtUtil.generateToken(username);
        String basicAuth = Base64.getEncoder().encodeToString("admin:1212".getBytes());

        // When & Then - JWT가 우선 처리되어 ROLE_USER로 인증됨
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validJWT)
                .header("Authorization", "Basic " + basicAuth)) // 이건 무시됨
                .andExpect(status().isNotFound()); // 사용자 없어서 404지만 JWT 인증은 통과
    }
}