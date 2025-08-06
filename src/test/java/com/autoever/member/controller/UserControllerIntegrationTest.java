package com.autoever.member.controller;

import com.autoever.member.config.TestSecurityConfig;
import com.autoever.member.dto.UserRegistrationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "async.enabled=false")
@DisplayName("UserController 통합 테스트")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."))
                .andExpect(jsonPath("$.data.username").value("testuser123"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.socialNumber").value("901201-*******")) // 마스킹된 주민등록번호
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 Bad Request 응답")
    void registerUser_MissingRequiredFields() throws Exception {
        // given - username 누락
        UserRegistrationDto requestDto = new UserRegistrationDto(
            null,
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
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("비밀번호 확인 불일치 시 400 Bad Request 응답")
    void registerUser_PasswordMismatch() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "DifferentPassword456!",
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
                .andExpect(jsonPath("$.data").exists());
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
            "invalid-email-format",
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
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("사용자명 형식 오류 시 400 Bad Request 응답")
    void registerUser_InvalidUsernameFormat() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "invalid-username!", // 특수문자 포함 (밑줄 제외)
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
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("전화번호 형식 오류 시 400 Bad Request 응답")
    void registerUser_InvalidPhoneNumberFormat() throws Exception {
        // given
        UserRegistrationDto requestDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-12345678", // 잘못된 형식 (하이픈 누락)
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
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("사용자명 중복 시 409 Conflict 응답")
    void registerUser_DuplicateUsername() throws Exception {
        // given - 첫 번째 사용자 등록
        UserRegistrationDto firstUser = new UserRegistrationDto(
            "duplicateuser",
            "Password123!",
            "Password123!",
            "첫번째사용자",
            "901201-1234567",
            "first@example.com",
            "010-1111-1111",
            "서울특별시 강남구 테헤란로 111"
        );

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated());

        // 두 번째 사용자 등록 (같은 사용자명)
        UserRegistrationDto secondUser = new UserRegistrationDto(
            "duplicateuser", // 중복된 사용자명
            "Password456!",
            "Password456!",
            "두번째사용자",
            "850101-2345678",
            "second@example.com",
            "010-2222-2222",
            "부산광역시 해운대구 센텀로 222"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 사용자명입니다: duplicateuser"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("주민등록번호 중복 시 409 Conflict 응답")
    void registerUser_DuplicateSocialNumber() throws Exception {
        // given - 첫 번째 사용자 등록
        UserRegistrationDto firstUser = new UserRegistrationDto(
            "firstuser",
            "Password123!",
            "Password123!",
            "첫번째사용자",
            "901201-1234567",
            "first@example.com",
            "010-1111-1111",
            "서울특별시 강남구 테헤란로 111"
        );

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated());

        // 두 번째 사용자 등록 (같은 주민등록번호)
        UserRegistrationDto secondUser = new UserRegistrationDto(
            "seconduser",
            "Password456!",
            "Password456!",
            "두번째사용자",
            "901201-1234567", // 중복된 주민등록번호
            "second@example.com",
            "010-2222-2222",
            "부산광역시 해운대구 센텀로 222"
        );

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 등록된 주민등록번호입니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("잘못된 JSON 형식 시 400 Bad Request 응답")
    void registerUser_InvalidJsonFormat() throws Exception {
        // given
        String invalidJson = "{ \"username\": \"testuser\", \"password\": }"; // 잘못된 JSON

        // when & then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
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
                .content(objectMapper.writeValueAsString(requestDto))) // Content-Type 없음
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
}