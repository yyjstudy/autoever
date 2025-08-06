package com.autoever.member.controller;

import com.autoever.member.dto.LoginDto;
import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.entity.User;
import com.autoever.member.jwt.JwtUtil;
import com.autoever.member.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController 로그인 통합 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private User testUser;
    private final String TEST_USERNAME = "testuser";
    private final String TEST_PASSWORD = "Test1234!";
    
    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .username(TEST_USERNAME)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .name("테스트 사용자")
                .socialNumber("900101-1234567")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .address("서울특별시 강남구 테헤란로 123")
                .build();
        userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("정상적인 로그인 요청 - JWT 토큰 발급")
    void login_ValidCredentials_ReturnsJwtToken() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto(TEST_USERNAME, TEST_PASSWORD);
        
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인이 성공적으로 완료되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andReturn();
        
        // JWT 토큰 검증
        String response = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("data").get("accessToken").asText();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(TEST_USERNAME);
    }
    
    @Test
    @DisplayName("잘못된 사용자명으로 로그인 시도 - 401 Unauthorized")
    void login_InvalidUsername_Returns401() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto("wronguser", TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자명 또는 비밀번호가 올바르지 않습니다."));
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시도 - 401 Unauthorized")
    void login_InvalidPassword_Returns401() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto(TEST_USERNAME, "WrongPassword!");
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자명 또는 비밀번호가 올바르지 않습니다."));
    }
    
    @Test
    @DisplayName("사용자명 없이 로그인 요청 - 400 Bad Request")
    void login_MissingUsername_Returns400() throws Exception {
        // Given
        String invalidJson = "{\"password\":\"Test1234!\"}";
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."))
                .andExpect(jsonPath("$.data", hasItem(containsString("username"))));
    }
    
    @Test
    @DisplayName("비밀번호 없이 로그인 요청 - 400 Bad Request")
    void login_MissingPassword_Returns400() throws Exception {
        // Given
        String invalidJson = "{\"username\":\"testuser\"}";
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."))
                .andExpect(jsonPath("$.data", hasItem(containsString("password"))));
    }
    
    @Test
    @DisplayName("빈 로그인 정보로 요청 - 400 Bad Request")
    void login_EmptyCredentials_Returns400() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto("", "");
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."))
                .andExpect(jsonPath("$.data", hasSize(4))); // username과 password 각각 2개씩 에러
    }
    
    @Test
    @DisplayName("잘못된 JSON 형식으로 로그인 요청 - 400 Bad Request")
    void login_MalformedJson_Returns400() throws Exception {
        // Given
        String malformedJson = "{invalid json}";
        
        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요."));
    }
    
    @Test
    @DisplayName("로그인 후 발급받은 토큰으로 보호된 엔드포인트 접근")
    void login_ThenAccessProtectedEndpoint_Success() throws Exception {
        // Given - 로그인
        LoginDto loginDto = new LoginDto(TEST_USERNAME, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("data").get("accessToken").asText();
        
        // When & Then - 보호된 엔드포인트 접근
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용자 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.socialNumber").value("900101-1******"))
                .andExpect(jsonPath("$.data.address").value("서울특별시"));
    }
}