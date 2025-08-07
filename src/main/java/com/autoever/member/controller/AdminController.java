package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.dto.UserUpdateDto;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 관리자 전용 API 컨트롤러
 * 관리자 권한이 필요한 회원 관리 기능을 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "관리자 전용 API - 회원 관리, 시스템 관리")
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final MessageSendTracker messageSendTracker;

    /**
     * 전체 회원 목록 조회 API (페이징 지원)
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준 (예: id,desc 또는 username,asc)
     * @return 페이징된 회원 목록
     */
    @GetMapping("/users")
    @Operation(
        summary = "전체 회원 목록 조회",
        description = "관리자 권한으로 전체 회원 목록을 페이징하여 조회합니다. 민감정보는 마스킹 처리됩니다.",
        parameters = {
            @Parameter(
                name = "page",
                description = "페이지 번호 (0부터 시작)",
                example = "0",
                schema = @Schema(type = "integer", defaultValue = "0")
            ),
            @Parameter(
                name = "size", 
                description = "페이지 크기 (한 페이지에 표시할 항목 수)",
                example = "10",
                schema = @Schema(type = "integer", defaultValue = "10")
            ),
            @Parameter(
                name = "sort",
                description = "정렬 기준 (속성명,방향). 방향은 asc(오름차순) 또는 desc(내림차순)",
                example = "id,desc",
                schema = @Schema(type = "string", defaultValue = "id,desc")
            )
        }
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "회원 목록 조회가 완료되었습니다.",
                          "data": {
                            "content": [
                              {
                                "id": 1,
                                "username": "testuser123",
                                "name": "홍길동",
                                "socialNumber": "901201-*******",
                                "email": "test@example.com",
                                "phoneNumber": "010-****-5678",
                                "address": "서울특별시 강남구 테헤란로 123"
                              },
                              {
                                "id": 2,
                                "username": "user456",
                                "name": "김철수",
                                "socialNumber": "850315-*******",
                                "email": "kim@example.com",
                                "phoneNumber": "010-****-5432",
                                "address": "부산광역시 해운대구 센텀로 100"
                              }
                            ],
                            "pageable": {
                              "pageNumber": 0,
                              "pageSize": 10,
                              "sort": {
                                "sorted": true,
                                "orderBy": "id"
                              }
                            },
                            "totalElements": 25,
                            "totalPages": 3,
                            "first": true,
                            "last": false
                          },
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "인증 실패 예시",
                    value = """
                        {
                          "success": false,
                          "message": "인증이 필요합니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "권한 부족 예시",
                    value = """
                        {
                          "success": false,
                          "message": "관리자 권한이 필요합니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        
        log.info("관리자 회원 목록 조회 요청: page={}, size={}, sort={}", page, size, sort);
        
        // 정렬 파라미터 파싱
        Pageable pageable = createPageable(page, size, sort);
        
        Page<UserResponseDto> users = adminService.getAllUsers(pageable);
        
        log.info("관리자 회원 목록 조회 완료: totalElements={}, totalPages={}", 
            users.getTotalElements(), users.getTotalPages());
        
        ApiResponse<Page<UserResponseDto>> response = ApiResponse.success(
            "회원 목록 조회가 완료되었습니다.", 
            users
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 페이징 파라미터를 Pageable 객체로 변환
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 기준 문자열 (예: "id,desc" 또는 "username,asc")
     * @return Pageable 객체
     */
    private Pageable createPageable(int page, int size, String sort) {
        // 페이지 크기 제한 (최대 100개)
        size = Math.min(size, 100);
        
        // 정렬 파라미터 파싱
        String[] sortParams = sort.split(",");
        String property = sortParams.length > 0 ? sortParams[0].trim() : "id";
        String direction = sortParams.length > 1 ? sortParams[1].trim() : "desc";
        
        // 유효한 정렬 속성인지 검증
        if (!isValidSortProperty(property)) {
            property = "id"; // 기본값으로 설정
        }
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return PageRequest.of(page, size, Sort.by(sortDirection, property));
    }
    
    /**
     * 유효한 정렬 속성인지 검증
     * 
     * @param property 정렬 속성명
     * @return 유효성 여부
     */
    private boolean isValidSortProperty(String property) {
        // User 엔티티의 유효한 속성들만 허용
        return List.of("id", "username", "name", "email", "createdAt", "updatedAt")
                   .contains(property);
    }

    /**
     * 특정 회원 상세 정보 조회 API
     * 
     * @param id 조회할 회원의 ID
     * @return 회원 상세 정보
     */
    @GetMapping("/users/{id}")
    @Operation(
        summary = "회원 상세 정보 조회",
        description = "관리자 권한으로 특정 회원의 상세 정보를 조회합니다. 민감정보는 마스킹 처리됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원 상세 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "회원 정보 조회가 완료되었습니다.",
                          "data": {
                            "id": 1,
                            "username": "testuser123",
                            "name": "홍길동",
                            "socialNumber": "901201-*******",
                            "email": "test@example.com",
                            "phoneNumber": "010-1234-5678",
                            "address": "서울특별시 강남구 테헤란로 123"
                          },
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "회원 없음 예시",
                    value = """
                        {
                          "success": false,
                          "message": "ID가 999인 사용자를 찾을 수 없습니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요"
        )
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(
            @Parameter(description = "조회할 회원의 ID", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("관리자 회원 상세 조회 요청: userId={}", id);
        
        UserResponseDto user = adminService.getUserById(id);
        
        log.info("관리자 회원 상세 조회 완료: userId={}, username={}", user.id(), user.username());
        
        ApiResponse<UserResponseDto> response = ApiResponse.success(
            "회원 정보 조회가 완료되었습니다.", 
            user
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 정보 수정 API
     * 
     * @param id 수정할 회원의 ID
     * @param updateDto 수정할 정보 (선택적 필드)
     * @return 수정된 회원 정보
     */
    @PutMapping("/users/{id}")
    @Operation(
        summary = "회원 정보 수정",
        description = """
            관리자 권한으로 특정 회원의 정보를 수정합니다.
            
            **수정 가능한 필드**:
            - 주소 (address)
            - 비밀번호 (password)
            
            **참고사항**:
            - null인 필드는 수정하지 않습니다
            - 최소 하나 이상의 필드가 제공되어야 합니다
            - 주소만, 비밀번호만, 또는 둘 다 수정 가능합니다
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 회원 정보 (암호/주소만 수정 가능)",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserUpdateDto.class),
                examples = {
                    @ExampleObject(
                        name = "비밀번호만 수정",
                        description = "비밀번호만 변경하는 경우",
                        value = """
                            {
                              "address": null,
                              "password": "NewPassword123!"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "주소만 수정",
                        description = "주소만 변경하는 경우",
                        value = """
                            {
                              "address": "부산광역시 해운대구 센텀로 200",
                              "password": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "주소와 비밀번호 동시 수정",
                        description = "주소와 비밀번호를 모두 수정하는 경우",
                        value = """
                            {
                              "address": "대구광역시 수성구 동대구로 100",
                              "password": "UpdatedPassword123!"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원 정보 수정 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "회원 정보가 성공적으로 수정되었습니다.",
                          "data": {
                            "id": 1,
                            "username": "testuser123",
                            "name": "홍길동",
                            "socialNumber": "901201-*******",
                            "email": "test@example.com",
                            "phoneNumber": "010-****-5678",
                            "address": "대구광역시 수성구 동대구로 100"
                          },
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "검증 오류 예시",
                    value = """
                        {
                          "success": false,
                          "message": "입력 값이 올바르지 않습니다.",
                          "data": {
                            "address": "주소는 5-500자 사이여야 합니다",
                            "password": "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다"
                          },
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "회원 없음 예시",
                    value = """
                        {
                          "success": false,
                          "message": "ID가 999인 사용자를 찾을 수 없습니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "중복 데이터로 인한 수정 실패 (현재 더 이상 사용되지 않음)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "예시",
                    value = """
                        {
                          "success": false,
                          "message": "오류 메시지",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요"
        )
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @Parameter(description = "수정할 회원의 ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto updateDto) {
        
        log.info("관리자 회원 정보 수정 요청: userId={}, updateFields={}", 
            id, getUpdateFields(updateDto));
        
        UserResponseDto updatedUser = adminService.updateUser(id, updateDto);
        
        log.info("관리자 회원 정보 수정 완료: userId={}, username={}", 
            updatedUser.id(), updatedUser.username());
        
        ApiResponse<UserResponseDto> response = ApiResponse.success(
            "회원 정보가 성공적으로 수정되었습니다.", 
            updatedUser
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 업데이트할 필드 목록을 문자열로 반환 (로깅용)
     */
    private String getUpdateFields(UserUpdateDto updateDto) {
        StringBuilder fields = new StringBuilder();
        if (updateDto.hasAddress()) fields.append("address,");
        if (updateDto.hasPassword()) fields.append("password,");
        
        return fields.length() > 0 
            ? fields.substring(0, fields.length() - 1) 
            : "none";
    }

    /**
     * 회원 삭제 API
     * 
     * @param id 삭제할 회원의 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/users/{id}")
    @Operation(
        summary = "회원 삭제",
        description = "관리자 권한으로 특정 회원을 시스템에서 완전히 삭제합니다. 이 작업은 되돌릴 수 없습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원 삭제 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "회원이 성공적으로 삭제되었습니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "회원 없음 예시",
                    value = """
                        {
                          "success": false,
                          "message": "ID가 999인 사용자를 찾을 수 없습니다.",
                          "data": null,
                          "timestamp": "2025-08-05 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요"
        )
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "삭제할 회원의 ID", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("관리자 회원 삭제 요청: userId={}", id);
        
        adminService.deleteUser(id);
        
        log.info("관리자 회원 삭제 완료: userId={}", id);
        
        ApiResponse<Void> response = ApiResponse.success(
            "회원이 성공적으로 삭제되었습니다.", 
            null
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 메시지 발송 통계 조회 API
     * 
     * @return 메시지 발송 통계 정보
     */
    @GetMapping("/messages/statistics")
    @Operation(
        summary = "메시지 발송 통계 조회",
        description = """
            관리자 권한으로 메시지 발송 통계를 조회합니다.
            
            **제공되는 정보**:
            - 전체 발송 시도 수
            - 성공률 (%)
            - Fallback 발생률 (%)
            - 채널별 성공/실패 건수
            - Rate Limiting 발생 건수
            
            **실시간 통계**: 시스템 시작 이후 누적 데이터를 제공합니다.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "메시지 발송 통계 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "메시지 발송 통계 조회가 완료되었습니다.",
                          "data": {
                            "totalAttempts": 1523,
                            "successRate": 94.7,
                            "fallbackRate": 23.1,
                            "kakaoSuccessCount": 1089,
                            "smsFallbackCount": 352,
                            "failedCount": 72,
                            "rateLimitedCount": 10,
                            "kakaoAttempts": 1523,
                            "smsAttempts": 424
                          },
                          "timestamp": "2025-08-07 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요"
        )
    })
    public ResponseEntity<ApiResponse<MessageSendTracker.SendStatistics>> getMessageStatistics() {
        log.info("관리자 메시지 발송 통계 조회 요청");
        
        MessageSendTracker.SendStatistics statistics = messageSendTracker.getStatistics();
        
        log.info("관리자 메시지 발송 통계 조회 완료: {}", statistics);
        
        ApiResponse<MessageSendTracker.SendStatistics> response = ApiResponse.success(
            "메시지 발송 통계 조회가 완료되었습니다.", 
            statistics
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 메시지 발송 통계 초기화 API
     * 
     * @return 초기화 완료 메시지
     */
    @PostMapping("/messages/statistics/reset")
    @Operation(
        summary = "메시지 발송 통계 초기화",
        description = """
            관리자 권한으로 메시지 발송 통계를 초기화합니다.
            
            **주의사항**:
            - 모든 누적 통계가 0으로 재설정됩니다
            - 이 작업은 되돌릴 수 없습니다
            - 시스템 성능에는 영향을 주지 않습니다
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "메시지 발송 통계 초기화 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                          "success": true,
                          "message": "메시지 발송 통계가 초기화되었습니다.",
                          "data": null,
                          "timestamp": "2025-08-07 15:30:45"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 부족 - 관리자 권한 필요"
        )
    })
    public ResponseEntity<ApiResponse<Void>> resetMessageStatistics() {
        log.info("관리자 메시지 발송 통계 초기화 요청");
        
        messageSendTracker.reset();
        
        log.info("관리자 메시지 발송 통계 초기화 완료");
        
        ApiResponse<Void> response = ApiResponse.success(
            "메시지 발송 통계가 초기화되었습니다.", 
            null
        );
        
        return ResponseEntity.ok(response);
    }
}