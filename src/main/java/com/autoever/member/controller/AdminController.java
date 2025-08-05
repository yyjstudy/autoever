package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.dto.UserResponseDto;
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
}