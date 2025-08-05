package com.autoever.member.handler;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private Object testObject;
    private BeanPropertyBindingResult bindingResult;

    @BeforeEach
    void setUp() {
        testObject = new Object();
        bindingResult = new BeanPropertyBindingResult(testObject, "testObject");
    }

    // ===== 커스텀 예외 처리 테스트 =====

    @Test
    @DisplayName("BaseCustomException 처리 테스트")
    void handleBaseCustomExceptionTest() {
        // given
        BaseCustomException exception = new TestCustomException("테스트 예외", HttpStatus.BAD_REQUEST, "TEST_ERROR");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBaseCustomException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("테스트 예외");
    }

    @Test
    @DisplayName("UserNotFoundException 처리 테스트")
    void handleUserNotFoundExceptionTest() {
        // given
        UserNotFoundException exception = new UserNotFoundException(123L);
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleUserNotFoundException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("사용자를 찾을 수 없습니다");
        assertThat(response.getBody().message()).contains("123");
    }

    @Test
    @DisplayName("DuplicateAccountException 처리 테스트")
    void handleDuplicateAccountExceptionTest() {
        // given
        DuplicateAccountException exception = new DuplicateAccountException("testuser");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleDuplicateAccountException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("testuser");
    }

    @Test
    @DisplayName("DuplicateSocialNumberException 처리 테스트")
    void handleDuplicateSocialNumberExceptionTest() {
        // given
        DuplicateSocialNumberException exception = new DuplicateSocialNumberException();
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleDuplicateSocialNumberException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("주민등록번호");
    }

    @Test
    @DisplayName("InvalidCredentialsException 처리 테스트")
    void handleInvalidCredentialsExceptionTest() {
        // given
        InvalidCredentialsException exception = new InvalidCredentialsException();
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidCredentialsException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("사용자명 또는 비밀번호");
    }

    @Test
    @DisplayName("AccessDeniedException 처리 테스트")
    void handleAccessDeniedExceptionTest() {
        // given
        AccessDeniedException exception = new AccessDeniedException("관리자 페이지");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleAccessDeniedException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("관리자 페이지");
    }

    @Test
    @DisplayName("BusinessLogicException 처리 테스트")
    void handleBusinessLogicExceptionTest() {
        // given
        BusinessLogicException exception = new BusinessLogicException("비즈니스 규칙 위반");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessLogicException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("비즈니스 규칙 위반");
    }

    @Test
    @DisplayName("InvalidRequestException 처리 테스트")
    void handleInvalidRequestExceptionTest() {
        // given
        InvalidRequestException exception = new InvalidRequestException("잘못된 요청");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidRequestException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("잘못된 요청");
    }

    // ===== Validation 예외 처리 테스트 =====

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 테스트")
    void handleMethodArgumentNotValidExceptionTest() {
        // given
        FieldError fieldError1 = new FieldError("user", "username", "사용자명은 필수입니다");
        FieldError fieldError2 = new FieldError("user", "password", "비밀번호는 8자 이상이어야 합니다");
        bindingResult.addError(fieldError1);
        bindingResult.addError(fieldError2);
        
        // Mock MethodParameter 생성
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        
        // when
        ResponseEntity<ApiResponse<List<String>>> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("입력값 검증에 실패했습니다.");
        assertThat(response.getBody().data()).hasSize(2);
        assertThat(response.getBody().data()).contains("username: 사용자명은 필수입니다");
        assertThat(response.getBody().data()).contains("password: 비밀번호는 8자 이상이어야 합니다");
    }

    @Test
    @DisplayName("ConstraintViolationException 처리 테스트")
    void handleConstraintViolationExceptionTest() {
        // given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        
        when(violation1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation1.getPropertyPath().toString()).thenReturn("id");
        when(violation1.getMessage()).thenReturn("ID는 양수여야 합니다");
        
        when(violation2.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation2.getPropertyPath().toString()).thenReturn("email");
        when(violation2.getMessage()).thenReturn("이메일 형식이 올바르지 않습니다");
        
        violations.add(violation1);
        violations.add(violation2);
        
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violations", violations);
        
        // when
        ResponseEntity<ApiResponse<List<String>>> response = globalExceptionHandler.handleConstraintViolationException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("제약조건 위반이 발생했습니다.");
        assertThat(response.getBody().data()).hasSize(2);
    }

    // ===== HTTP 요청 관련 예외 처리 테스트 =====

    @Test
    @DisplayName("MissingServletRequestParameterException 처리 테스트")
    void handleMissingServletRequestParameterExceptionTest() {
        // given
        MissingServletRequestParameterException exception = 
            new MissingServletRequestParameterException("username", "String");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleMissingServletRequestParameterException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("필수 파라미터가 누락되었습니다: username");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리 테스트")
    void handleMethodArgumentTypeMismatchExceptionTest() {
        // given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");
        when(exception.getMessage()).thenReturn("Type mismatch");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("파라미터 타입이 올바르지 않습니다: id");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException 처리 테스트")
    void handleHttpMessageNotReadableExceptionTest() {
        // given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleHttpMessageNotReadableException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("요청 본문을 읽을 수 없습니다");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException 처리 테스트")
    void handleHttpRequestMethodNotSupportedExceptionTest() {
        // given
        HttpRequestMethodNotSupportedException exception = 
            new HttpRequestMethodNotSupportedException("DELETE");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleHttpRequestMethodNotSupportedException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("지원하지 않는 HTTP 메서드입니다: DELETE");
    }

    @Test
    @DisplayName("NoResourceFoundException 처리 테스트")
    void handleNoResourceFoundExceptionTest() {
        // given
        NoResourceFoundException exception = mock(NoResourceFoundException.class);
        when(exception.getMessage()).thenReturn("No static resource found");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNoResourceFoundException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }

    // ===== 일반 예외 처리 테스트 =====

    @Test
    @DisplayName("IllegalArgumentException 처리 테스트")
    void handleIllegalArgumentExceptionTest() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인수입니다");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleIllegalArgumentException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("잘못된 인수입니다");
    }

    @Test
    @DisplayName("IllegalStateException 처리 테스트")
    void handleIllegalStateExceptionTest() {
        // given
        IllegalStateException exception = new IllegalStateException("잘못된 상태입니다");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleIllegalStateException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("시스템 상태가 올바르지 않습니다: 잘못된 상태입니다");
    }

    @Test
    @DisplayName("RuntimeException 처리 테스트")
    void handleRuntimeExceptionTest() {
        // given
        RuntimeException exception = new RuntimeException("예상치 못한 런타임 오류");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleRuntimeException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("Exception 최종 처리기 테스트")
    void handleExceptionTest() {
        // given
        Exception exception = new Exception("예상치 못한 예외");
        
        // when
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleException(exception);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("서버에서 예상치 못한 오류가 발생했습니다.");
    }

    // ===== 테스트용 커스텀 예외 클래스 =====
    
    private static class TestCustomException extends BaseCustomException {
        public TestCustomException(String message, HttpStatus httpStatus, String errorCode) {
            super(message, httpStatus, errorCode);
        }
    }
}