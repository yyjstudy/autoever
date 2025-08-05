package com.autoever.member.controller;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.exception.DuplicateAccountException;
import com.autoever.member.exception.DuplicateSocialNumberException;
import com.autoever.member.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Import(UserControllerTest.TestSecurityConfig.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("정상적인 회원가입 요청 시 201 Created 응답")
    void registerUser_Success() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        UserResponseDto responseDto = new UserResponseDto(
            1L,
            "testuser123",
            "홍길동",
            "901201-*******",
            "010-****-5678",
            "서울특별시",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now()
        );

        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser123"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.socialNumber").value("901201-*******"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-****-5678"))
                .andExpect(jsonPath("$.data.address").value("서울특별시"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 Bad Request 응답")
    void registerUser_MissingRequiredFields() throws Exception {
        // given - username이 누락된 경우
        UserRegistrationDto requestDto = new UserRegistrationDto(
            null, // username 누락
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("사용자명 형식 오류 시 400 Bad Request 응답")
    void registerUser_InvalidUsernameFormat() throws Exception {
        // given - 사용자명에 특수문자 포함
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "test@user", // 잘못된 형식
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));
    }

    @Test
    @DisplayName("비밀번호 확인 불일치 시 400 Bad Request 응답")
    void registerUser_PasswordMismatch() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "DifferentPassword!", // 비밀번호 불일치
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호와 비밀번호 확인이 일치하지 않습니다"));
    }

    @Test
    @DisplayName("사용자명 중복 시 409 Conflict 응답")
    void registerUser_DuplicateUsername() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        when(userService.registerUser(any(UserRegistrationDto.class)))
            .thenThrow(new DuplicateAccountException("testuser123"));

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 사용자명입니다: testuser123"));
    }

    @Test
    @DisplayName("주민등록번호 중복 시 409 Conflict 응답")
    void registerUser_DuplicateSocialNumber() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        when(userService.registerUser(any(UserRegistrationDto.class)))
            .thenThrow(new DuplicateSocialNumberException());

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 등록된 주민등록번호입니다."));
    }

    @Test
    @DisplayName("잘못된 JSON 형식 시 400 Bad Request 응답")
    void registerUser_InvalidJsonFormat() throws Exception {
        // given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요."));
    }

    @Test
    @DisplayName("Content-Type이 없는 경우 415 Unsupported Media Type 응답")
    void registerUser_NoContentType() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .content(objectMapper.writeValueAsString(requestDto))) // Content-Type 미설정
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("이메일 형식 오류 시 400 Bad Request 응답")
    void registerUser_InvalidEmailFormat() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "invalid-email", // 잘못된 이메일 형식
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));
    }

    @Test
    @DisplayName("전화번호 형식 오류 시 400 Bad Request 응답")
    void registerUser_InvalidPhoneFormat() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "01012345678", // 잘못된 전화번호 형식
            "서울특별시 강남구 테헤란로 123"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}