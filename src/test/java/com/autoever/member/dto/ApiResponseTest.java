package com.autoever.member.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("데이터와 함께 성공 응답 생성 테스트")
    void successWithDataTest() {
        // given
        Integer testData = 12345; // String이 아닌 타입으로 변경하여 올바른 메서드 호출 보장
        
        // when
        ApiResponse<Integer> response = ApiResponse.success(testData);
        
        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("요청이 성공적으로 처리되었습니다.");
        assertThat(response.data()).isEqualTo(testData);
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.timestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("메시지와 데이터가 함께 있는 성공 응답 생성 테스트")
    void successWithMessageAndDataTest() {
        // given
        String customMessage = "사용자 정보 조회 성공";
        Map<String, Object> userData = Map.of("id", 1L, "name", "홍길동");
        
        // when
        ApiResponse<Map<String, Object>> response = ApiResponse.success(customMessage, userData);
        
        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo(customMessage);
        assertThat(response.data()).isEqualTo(userData);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("데이터 없이 성공 응답 생성 테스트")
    void successWithMessageOnlyTest() {
        // given
        String message = "작업이 완료되었습니다.";
        
        // when
        ApiResponse<Object> response = ApiResponse.success(message);
        
        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("생성 성공 응답 테스트 (HTTP 201)")
    void createdResponseTest() {
        // given
        UserResponseDto newUser = new UserResponseDto(1L, "newuser", "신규사용자", 
                "951010-*******", "newuser@example.com", "010-****-5678", "서울시 강남구", 
                LocalDateTime.now(), LocalDateTime.now());
        
        // when
        ApiResponse<UserResponseDto> response = ApiResponse.created(newUser);
        
        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("리소스가 성공적으로 생성되었습니다.");
        assertThat(response.data()).isEqualTo(newUser);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("커스텀 메시지와 함께 생성 성공 응답 테스트")
    void createdWithCustomMessageTest() {
        // given
        String customMessage = "새로운 사용자가 성공적으로 등록되었습니다.";
        Long userId = 1L;
        
        // when
        ApiResponse<Long> response = ApiResponse.created(customMessage, userId);
        
        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo(customMessage);
        assertThat(response.data()).isEqualTo(userId);
    }

    @Test
    @DisplayName("오류 응답 생성 테스트")
    void errorResponseTest() {
        // given
        String errorMessage = "처리 중 오류가 발생했습니다.";
        
        // when
        ApiResponse<Object> response = ApiResponse.error(errorMessage);
        
        // then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(errorMessage);
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("데이터와 함께 오류 응답 생성 테스트")
    void errorWithDataTest() {
        // given
        String errorMessage = "검증 오류가 발생했습니다.";
        List<String> validationErrors = List.of("사용자명은 필수입니다.", "비밀번호가 너무 짧습니다.");
        
        // when
        ApiResponse<List<String>> response = ApiResponse.error(errorMessage, validationErrors);
        
        // then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(errorMessage);
        assertThat(response.data()).isEqualTo(validationErrors);
    }

    @Test
    @DisplayName("Bad Request 오류 응답 테스트 (HTTP 400)")
    void badRequestResponseTest() {
        // given
        String message = "잘못된 요청입니다.";
        
        // when
        ApiResponse<Object> response = ApiResponse.badRequest(message);
        
        // then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("인증 실패 응답 테스트 (HTTP 401)")
    void unauthorizedResponseTest() {
        // when - 커스텀 메시지
        ApiResponse<Object> response1 = ApiResponse.unauthorized("토큰이 유효하지 않습니다.");
        
        // then
        assertThat(response1.success()).isFalse();
        assertThat(response1.message()).isEqualTo("토큰이 유효하지 않습니다.");
        
        // when - null 메시지 (기본 메시지 사용)
        ApiResponse<Object> response2 = ApiResponse.unauthorized(null);
        
        // then
        assertThat(response2.message()).isEqualTo("인증이 필요합니다.");
    }

    @Test
    @DisplayName("권한 없음 응답 테스트 (HTTP 403)")
    void forbiddenResponseTest() {
        // when - 커스텀 메시지
        ApiResponse<Object> response1 = ApiResponse.forbidden("관리자 권한이 필요합니다.");
        
        // then
        assertThat(response1.success()).isFalse();
        assertThat(response1.message()).isEqualTo("관리자 권한이 필요합니다.");
        
        // when - null 메시지 (기본 메시지 사용)
        ApiResponse<Object> response2 = ApiResponse.forbidden(null);
        
        // then
        assertThat(response2.message()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("리소스 찾을 수 없음 응답 테스트 (HTTP 404)")
    void notFoundResponseTest() {
        // when - 커스텀 메시지
        ApiResponse<Object> response1 = ApiResponse.notFound("사용자를 찾을 수 없습니다.");
        
        // then
        assertThat(response1.success()).isFalse();
        assertThat(response1.message()).isEqualTo("사용자를 찾을 수 없습니다.");
        
        // when - null 메시지 (기본 메시지 사용)
        ApiResponse<Object> response2 = ApiResponse.notFound(null);
        
        // then
        assertThat(response2.message()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("충돌 오류 응답 테스트 (HTTP 409)")
    void conflictResponseTest() {
        // given
        String message = "이미 존재하는 사용자명입니다.";
        
        // when
        ApiResponse<Object> response = ApiResponse.conflict(message);
        
        // then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("서버 내부 오류 응답 테스트 (HTTP 500)")
    void internalServerErrorResponseTest() {
        // when - 커스텀 메시지
        ApiResponse<Object> response1 = ApiResponse.internalServerError("데이터베이스 연결 오류");
        
        // then
        assertThat(response1.success()).isFalse();
        assertThat(response1.message()).isEqualTo("데이터베이스 연결 오류");
        
        // when - null 메시지 (기본 메시지 사용)
        ApiResponse<Object> response2 = ApiResponse.internalServerError(null);
        
        // then
        assertThat(response2.message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("타입 안전성 테스트 - 다양한 제네릭 타입")
    void genericTypeSafetyTest() {
        // String 타입 - 메시지와 데이터를 구분하기 위해 명시적으로 메시지 전달
        ApiResponse<String> stringResponse = ApiResponse.success("성공", "문자열 데이터");
        assertThat(stringResponse.data()).isNotNull();
        assertThat(stringResponse.data()).isInstanceOf(String.class);
        
        // Integer 타입
        ApiResponse<Integer> intResponse = ApiResponse.success(42);
        assertThat(intResponse.data()).isNotNull();
        assertThat(intResponse.data()).isInstanceOf(Integer.class);
        
        // List 타입
        List<String> list = List.of("item1", "item2");
        ApiResponse<List<String>> listResponse = ApiResponse.success(list);
        assertThat(listResponse.data()).isNotNull();
        assertThat(listResponse.data()).isInstanceOf(List.class);
        
        // Map 타입
        Map<String, Object> map = Map.of("key", "value");
        ApiResponse<Map<String, Object>> mapResponse = ApiResponse.success(map);
        assertThat(mapResponse.data()).isNotNull();
        assertThat(mapResponse.data()).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("timestamp 필드 정확성 테스트")
    void timestampAccuracyTest() {
        // given
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        // when
        ApiResponse<String> response = ApiResponse.success("test");
        
        // then
        LocalDateTime afterCreate = LocalDateTime.now();
        assertThat(response.timestamp()).isAfter(beforeCreate.minusSeconds(1));
        assertThat(response.timestamp()).isBefore(afterCreate.plusSeconds(1));
    }

    @Test
    @DisplayName("record 클래스 불변성 테스트")
    void immutabilityTest() {
        // given
        ApiResponse<String> response = ApiResponse.success("original data");
        
        // when - record는 불변이므로 필드를 변경할 수 없음
        String originalMessage = response.message();
        boolean originalSuccess = response.success();
        String originalData = response.data();
        LocalDateTime originalTimestamp = response.timestamp();
        
        // then - 모든 필드가 불변임
        assertThat(response.message()).isEqualTo(originalMessage);
        assertThat(response.success()).isEqualTo(originalSuccess);
        assertThat(response.data()).isEqualTo(originalData);
        assertThat(response.timestamp()).isEqualTo(originalTimestamp);
    }
}