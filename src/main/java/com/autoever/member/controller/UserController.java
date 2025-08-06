package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.dto.JwtTokenDto;
import com.autoever.member.dto.LoginDto;
import com.autoever.member.dto.UserInfoDto;
import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API 컨트롤러
 * 회원가입, 로그인, 개인정보 조회 등의 엔드포인트를 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 API - 회원가입, 로그인, 개인정보 관리")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 API
     * 
     * @param registrationDto 회원가입 요청 데이터
     * @return 생성된 사용자 정보 (비밀번호 제외)
     */
    @PostMapping("/register")
    @Operation(
        summary = "회원가입",
        description = "새로운 사용자 계정을 생성합니다. 사용자명과 주민등록번호는 중복될 수 없습니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "회원가입 요청 정보",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRegistrationDto.class),
                examples = @ExampleObject(
                    name = "회원가입 예시",
                    value = """
                        {
                          "username": "testuser123",
                          "password": "Password123!",
                          "confirmPassword": "Password123!",
                          "name": "홍길동",
                          "socialNumber": "901201-1234567",
                          "email": "test@example.com",
                          "phoneNumber": "010-1234-5678",
                          "address": "서울특별시 강남구 테헤란로 123"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "회원가입이 성공적으로 완료되었습니다.",
                          "data": {
                            "id": 1,
                            "username": "testuser123",
                            "name": "홍길동",
                            "socialNumber": "901201-*******",
                            "email": "test@example.com",
                            "phoneNumber": "010-1234-5678",
                            "address": "서울특별시"
                          },
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "입력값 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "검증 실패 예시",
                    value = """
                        {
                          "success": false,
                          "message": "입력값 검증에 실패했습니다.",
                          "data": [
                            "username: 사용자명은 3-50자 사이여야 합니다",
                            "password: 비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다"
                          ],
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "중복 데이터 오류 (사용자명 또는 주민등록번호 중복)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "중복 오류 예시",
                    value = """
                        {
                          "success": false,
                          "message": "이미 존재하는 사용자명입니다: testuser123",
                          "data": null,
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> register(
            @Parameter(description = "회원가입 요청 정보", required = true)
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        
        log.info("회원가입 요청: username={}", registrationDto.username());
        
        UserResponseDto createdUser = userService.registerUser(registrationDto);
        
        log.info("회원가입 성공: userId={}, username={}", createdUser.id(), createdUser.username());
        
        ApiResponse<UserResponseDto> response = ApiResponse.created(
            "회원가입이 성공적으로 완료되었습니다.", 
            createdUser
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 로그인 API
     * 
     * @param loginDto 로그인 요청 데이터 (username, password)
     * @return JWT 토큰 정보
     */
    @PostMapping("/login")
    @Operation(
        summary = "사용자 로그인",
        description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그인 요청 정보",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginDto.class),
                examples = @ExampleObject(
                    name = "로그인 요청 예시",
                    value = """
                        {
                          "username": "testuser123",
                          "password": "Password123!"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "로그인이 성공적으로 완료되었습니다.",
                          "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "tokenType": "Bearer",
                            "expiresIn": 86400
                          },
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "입력값 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "검증 실패 예시",
                    value = """
                        {
                          "success": false,
                          "message": "입력값 검증에 실패했습니다.",
                          "data": [
                            "username: 사용자명은 필수입니다",
                            "password: 비밀번호는 필수입니다"
                          ],
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "인증 실패 예시",
                    value = """
                        {
                          "success": false,
                          "message": "사용자명 또는 비밀번호가 올바르지 않습니다.",
                          "data": null,
                          "timestamp": "2025-08-05 13:45:23"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<JwtTokenDto>> login(
            @Parameter(description = "로그인 요청 정보", required = true)
            @Valid @RequestBody LoginDto loginDto) {
        
        log.info("로그인 요청: username={}", loginDto.username());
        
        JwtTokenDto tokenDto = userService.login(loginDto);
        
        log.info("로그인 성공: username={}", loginDto.username());
        
        ApiResponse<JwtTokenDto> response = ApiResponse.success(
            "로그인이 성공적으로 완료되었습니다.", 
            tokenDto
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 본인 정보 조회 API
     * 
     * @return 마스킹된 사용자 개인정보
     */
    @GetMapping("/me")
    @Operation(
        summary = "본인 정보 조회",
        description = "현재 로그인한 사용자의 개인정보를 조회합니다. 민감정보는 마스킹 처리되어 응답됩니다.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "성공 응답 예시",
                        value = """
                            {
                              "success": true,
                              "message": "사용자 정보를 성공적으로 조회했습니다.",
                              "data": {
                                "id": 1,
                                "username": "testuser123",
                                "name": "홍길동",
                                "socialNumber": "901201-1******",
                                "email": "user@example.com",
                                "phoneNumber": "010-1234-5678",
                                "address": "서울특별시",
                                "createdAt": "2025-08-05T10:30:00",
                                "updatedAt": "2025-08-05T15:45:30"
                              },
                              "timestamp": "2025-08-05 16:30:45"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패 - 유효하지 않거나 만료된 JWT 토큰",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "인증 실패 예시",
                        value = """
                            {
                              "success": false,
                              "message": "인증이 필요합니다.",
                              "data": null,
                              "timestamp": "2025-08-05 16:30:45"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자 정보를 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "사용자 없음 예시",
                        value = """
                            {
                              "success": false,
                              "message": "사용자를 찾을 수 없습니다: testuser123",
                              "data": null,
                              "timestamp": "2025-08-05 16:30:45"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<UserInfoDto>> getCurrentUserInfo() {
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        log.info("본인 정보 조회 요청: username={}", username);
        
        UserInfoDto userInfo = userService.getCurrentUserInfo(username);
        
        log.info("본인 정보 조회 성공: username={}", username);
        
        ApiResponse<UserInfoDto> response = ApiResponse.success(
            "사용자 정보를 성공적으로 조회했습니다.", 
            userInfo
        );
        
        return ResponseEntity.ok(response);
    }
}