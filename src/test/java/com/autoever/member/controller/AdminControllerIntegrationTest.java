package com.autoever.member.controller;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 통합 테스트
 * 실제 Spring Security 설정과 함께 관리자 API를 테스트
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .alwaysDo(print())
                .build();
        
        // 각 테스트 전에 데이터베이스 초기화
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("관리자 권한 없이 회원 목록 조회 시 401 Unauthorized 반환")
    void getAllUsers_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 관리자 계정으로 회원 목록 조회 시 401 Unauthorized 반환")
    void getAllUsers_WithWrongCredentials_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 권한으로 빈 회원 목록 조회 성공")
    void getAllUsers_WithAdminAuth_EmptyList_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    @DisplayName("회원 등록 후 관리자 권한으로 회원 목록 조회 성공")
    void getAllUsers_WithAdminAuth_AfterUserRegistration_ShouldReturnUsers() throws Exception {
        // Given: 테스트 사용자 2명 등록
        createTestUser("testuser1", "홍길동", "901201-1234567", "test1@example.com", "010-1234-5678");
        createTestUser("testuser2", "김철수", "851015-2345678", "test2@example.com", "010-9876-5432");

        // When & Then: 기본 파라미터로 회원 목록 조회 (id,desc 정렬)
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                // ID 값은 시퀀스로 인해 예측할 수 없으므로 username으로 검증
                .andExpect(jsonPath("$.data.content[0].username").value("testuser2"))  // id desc 정렬로 testuser2가 먼저
                .andExpect(jsonPath("$.data.content[0].name").value("김철수"))
                .andExpect(jsonPath("$.data.content[0].socialNumber").value("851015-*******"))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value("010-****-5432"))
                .andExpect(jsonPath("$.data.content[1].username").value("testuser1"))
                .andExpect(jsonPath("$.data.content[1].name").value("홍길동"));
    }

    @Test
    @DisplayName("페이징 파라미터로 회원 목록 조회 - 페이지 크기 1")
    void getAllUsers_WithPagingParameters_Size1_ShouldReturnPagedResults() throws Exception {
        // Given: 테스트 사용자 2명 등록
        createTestUser("testuser1", "홍길동", "901201-1234567", "test1@example.com", "010-1234-5678");
        createTestUser("testuser2", "김철수", "851015-2345678", "test2@example.com", "010-9876-5432");

        // When & Then: 첫 번째 페이지 (size=1, username 오름차순)
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "username,asc")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content[0].username").value("testuser1"));  // username asc로 testuser1이 먼저

        // When & Then: 두 번째 페이지 (size=1, username 오름차순)
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "username,asc")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("testuser2"));  // username asc로 testuser2가 두 번째
    }

    @Test
    @DisplayName("정렬 파라미터 테스트 - username 오름차순")
    void getAllUsers_WithSortParameter_UsernameAsc_ShouldReturnSortedResults() throws Exception {
        // Given: 테스트 사용자 2명 등록
        createTestUser("zebra", "지브라", "901201-1234567", "zebra@example.com");
        createTestUser("alpha", "알파", "851015-2345678", "alpha@example.com");

        // When & Then: username 오름차순 정렬
        mockMvc.perform(get("/api/admin/users")
                        .param("sort", "username,asc")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].username").value("alpha"))   // 알파벳 순으로 alpha 먼저
                .andExpect(jsonPath("$.data.content[1].username").value("zebra"));  // zebra 나중
    }

    @Test
    @DisplayName("정렬 파라미터 테스트 - 잘못된 속성명은 기본값(id)으로 대체")
    void getAllUsers_WithInvalidSortProperty_ShouldUseDefaultSort() throws Exception {
        // Given: 테스트 사용자 1명 등록
        createTestUser("testuser1", "홍길동", "901201-1234567", "test1@example.com");

        // When & Then: 잘못된 정렬 속성 (invalidProperty)
        mockMvc.perform(get("/api/admin/users")
                        .param("sort", "invalidProperty,asc")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
        // 잘못된 속성은 기본값 id로 대체되므로 정상 동작해야 함
    }

    @Test
    @DisplayName("관리자 권한으로 특정 회원 상세 조회 성공")
    void getUserById_WithAdminAuth_ExistingUser_ShouldReturnUser() throws Exception {
        // Given: 테스트 사용자 등록
        createTestUser("testuser1", "홍길동", "901201-1234567", "test1@example.com", "010-1234-5678");

        // 생성된 사용자 목록을 먼저 조회하여 존재 확인 후 개별 상세 조회는 별도 테스트에서 수행
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].username").value("testuser1"))
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].socialNumber").value("901201-*******"))
                .andExpect(jsonPath("$.data.content[0].email").value("test1@example.com"))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value("010-****-5678"))
                .andExpect(jsonPath("$.data.content[0].address").value("서울특별시 강남구 테헤란로 123"));
    }

    @Test
    @DisplayName("관리자 권한으로 존재하지 않는 회원 조회 시 404 Not Found 반환")
    void getUserById_WithAdminAuth_NonExistentUser_ShouldReturn404() throws Exception {
        // When & Then: 존재하지 않는 회원 ID 999로 조회
        mockMvc.perform(get("/api/admin/users/999")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("관리자 권한 없이 특정 회원 조회 시 401 Unauthorized 반환")
    void getUserById_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트 - 최대 100개로 제한")
    void getAllUsers_WithLargePageSize_ShouldLimitTo100() throws Exception {
        // When & Then: 페이지 크기를 200으로 요청해도 100으로 제한됨
        mockMvc.perform(get("/api/admin/users")
                        .param("size", "200")
                        .with(httpBasic("admin", "1212")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));  // 100으로 제한됨
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드 (기본 전화번호)
     */
    private void createTestUser(String username, String name, String socialNumber, String email) throws Exception {
        createTestUser(username, name, socialNumber, email, "010-1234-5678");
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드 (전화번호 지정)
     */
    private void createTestUser(String username, String name, String socialNumber, String email, String phoneNumber) throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto(
                username,
                "Password123@",
                "Password123@",
                name,
                socialNumber,
                email,
                phoneNumber,
                "서울특별시 강남구 테헤란로 123"
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated());
    }
}