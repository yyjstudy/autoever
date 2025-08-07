package com.autoever.test.controller;

import com.autoever.test.dto.TestUserCreateRequest;
import com.autoever.test.dto.TestUserCreateResponse;
import com.autoever.test.service.TestDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 테스트 데이터 관리 컨트롤러
 * H2 메모리 DB를 사용한 테스트 환경에서 데이터 생성/삭제용
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Test Data API", description = "테스트용 데이터 생성/삭제 API")
public class TestDataController {
    
    private final TestDataService testDataService;
    
    /**
     * 모든 유저 삭제
     */
    @DeleteMapping("/users")
    @Operation(
        summary = "모든 유저 삭제",
        description = """
            H2 메모리 데이터베이스의 모든 유저를 삭제합니다.
            
            **사용 시나리오:**
            - 테스트 데이터 초기화
            - 새로운 테스트 케이스 준비
            - 데이터베이스 클린업
            
            **주의사항:**
            - 이 작업은 되돌릴 수 없습니다
            - H2 메모리 DB 전용 (운영 환경에서는 사용 금지)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "모든 유저 삭제 완료", 
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "삭제 성공",
                            summary = "성공적인 삭제 응답",
                            value = """
                                {
                                  "status": "success",
                                  "message": "모든 유저가 성공적으로 삭제되었습니다."
                                }
                                """
                        )
                    )),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json", 
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "서버 오류",
                            summary = "삭제 실패 응답",
                            value = """
                                {
                                  "status": "error",
                                  "message": "유저 삭제 중 오류가 발생했습니다: 데이터베이스 연결 실패"
                                }
                                """
                        )
                    ))
    })
    public ResponseEntity<Map<String, String>> deleteAllUsers() {
        log.info("모든 유저 삭제 요청 받음");
        
        try {
            testDataService.deleteAllUsers();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "모든 유저가 성공적으로 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("유저 삭제 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "유저 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 랜덤 유저 생성
     */
    @PostMapping("/users")
    @Operation(
        summary = "랜덤 유저 생성",
        description = """
            지정된 수만큼 랜덤한 정보를 가진 유저를 생성합니다.
            
            **입력 파라미터:**
            - **userCount**: 생성할 유저 수 (1~100명)
            - **ageGroup**: 연령대 (선택사항)
              - 10: 10~19세 랜덤
              - 20: 20~29세 랜덤
              - 30: 30~39세 랜덤
              - ... (10단위로 90까지)
              - 미입력 시: 0~99세 완전 랜덤
            
            **생성되는 정보:**
            - 사용자명: testuser + 5자리 숫자
            - 패스워드: test123 (BCrypt 암호화)
            - 이름: 한국식 랜덤 이름
            - 주민등록번호: 연령대에 맞는 유효한 번호
            - 이메일: 사용자명@도메인 형태
            - 전화번호: 010-xxxx-xxxx 형태
            - 주소: 대한민국 실제 주소 기반 랜덤 생성
            
            **사용 예시:**
            - 특정 연령대 테스트: ageGroup=30으로 30대 유저만 생성
            - 다양한 연령대 테스트: ageGroup 없이 전체 연령 랜덤
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "유저 생성 완료",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "소규모 생성",
                                summary = "3명의 30대 유저 생성",
                                value = """
                                    {
                                      "createdCount": 3,
                                      "userIds": [1, 2, 3],
                                      "userSummaries": [
                                        {
                                          "id": 1,
                                          "username": "testuser12345",
                                          "name": "김민준",
                                          "phoneNumber": "010-1234-5678",
                                          "email": "testuser12345@gmail.com",
                                          "age": 32
                                        },
                                        {
                                          "id": 2,
                                          "username": "testuser67890",
                                          "name": "이서연",
                                          "phoneNumber": "010-9876-5432",
                                          "email": "testuser67890@naver.com",
                                          "age": 35
                                        },
                                        {
                                          "id": 3,
                                          "username": "testuser11111",
                                          "name": "박지우",
                                          "phoneNumber": "010-5555-7777",
                                          "email": "testuser11111@kakao.com",
                                          "age": 38
                                        }
                                      ]
                                    }
                                    """
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "대량 생성",
                                summary = "50명의 랜덤 연령 유저 생성",
                                value = """
                                    {
                                      "createdCount": 50,
                                      "userIds": [1, 2, 3, 4, 5, "...", 50],
                                      "userSummaries": [
                                        {
                                          "id": 1,
                                          "username": "testuser54321",
                                          "name": "최도윤",
                                          "phoneNumber": "010-2468-1357",
                                          "email": "testuser54321@daum.net",
                                          "age": 25
                                        },
                                        "... 49명 더"
                                      ]
                                    }
                                    """
                            )
                        }
                    )),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "잘못된 연령대",
                            summary = "유효하지 않은 연령대 입력",
                            value = """
                                {
                                  "status": "error",
                                  "message": "연령대는 10, 20, 30, ..., 90 중 하나여야 합니다."
                                }
                                """
                        )
                    )),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "생성 실패",
                            summary = "유저 생성 중 오류 발생",
                            value = """
                                {
                                  "status": "error",
                                  "message": "유저 생성 중 오류가 발생했습니다: 데이터베이스 제약 조건 위반"
                                }
                                """
                        )
                    ))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "유저 생성 요청 정보",
        required = true,
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "기본 예시",
                    summary = "10명의 30대 유저 생성",
                    value = """
                        {
                          "userCount": 10,
                          "ageGroup": 30
                        }
                        """
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "랜덤 연령",
                    summary = "5명의 랜덤 연령 유저 생성",
                    value = """
                        {
                          "userCount": 5
                        }
                        """
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "최대 생성",
                    summary = "100명의 20대 유저 생성",
                    value = """
                        {
                          "userCount": 100,
                          "ageGroup": 20
                        }
                        """
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "10대 생성",
                    summary = "15명의 10~19세 유저 생성",
                    value = """
                        {
                          "userCount": 15,
                          "ageGroup": 10
                        }
                        """
                )
            }
        )
    )
    public ResponseEntity<?> createRandomUsers(@Valid @RequestBody TestUserCreateRequest request) {
        log.info("랜덤 유저 생성 요청 받음 - 생성할 수: {}, 연령대: {}", 
                request.userCount(), request.ageGroup());
        
        try {
            // 연령대 유효성 검증
            if (request.ageGroup() != null) {
                int ageGroup = request.ageGroup();
                if (ageGroup % 10 != 0 || ageGroup < 10 || ageGroup > 90) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "연령대는 10, 20, 30, ..., 90 중 하나여야 합니다."
                    ));
                }
            }
            
            TestUserCreateResponse response = testDataService.createRandomUsers(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("유저 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "유저 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 테스트 API 상태 확인
     */
    @GetMapping("/health")
    @Operation(
        summary = "테스트 API 상태 확인",
        description = """
            테스트 API의 동작 상태를 확인합니다.
            
            **용도:**
            - API 서버 연결 상태 확인
            - 테스트 환경 정상 동작 여부 검증
            - 헬스체크 및 모니터링
            
            **응답 정보:**
            - status: API 동작 상태
            - message: 상태 메시지
            - timestamp: 응답 생성 시간
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "정상 동작 중",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "정상 상태",
                            summary = "API 정상 동작 중",
                            value = """
                                {
                                  "status": "OK",
                                  "message": "테스트 API가 정상 동작 중입니다.",
                                  "timestamp": 1691404800000
                                }
                                """
                        )
                    ))
    })
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "테스트 API가 정상 동작 중입니다.",
            "timestamp", System.currentTimeMillis()
        ));
    }
}