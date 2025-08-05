package com.autoever.member.handler;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 전역 예외 처리기
 * 애플리케이션 전반에서 발생하는 예외를 일관된 형태로 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 커스텀 예외 처리 =====

    /**
     * BaseCustomException을 상속한 모든 커스텀 예외 처리
     */
    @ExceptionHandler(BaseCustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleBaseCustomException(BaseCustomException ex) {
        log.warn("Custom exception occurred: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * 사용자 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.notFound(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 중복 계정 예외 처리
     */
    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateAccountException(DuplicateAccountException ex) {
        log.warn("Duplicate account attempt: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.conflict(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 중복 주민번호 예외 처리
     */
    @ExceptionHandler(DuplicateSocialNumberException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateSocialNumberException(DuplicateSocialNumberException ex) {
        log.warn("Duplicate social number attempt: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.conflict(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 인증 실패 예외 처리
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.unauthorized(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.forbidden(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessLogicException(BusinessLogicException ex) {
        log.warn("Business logic violation: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 잘못된 요청 예외 처리
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRequestException(InvalidRequestException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===== Validation 예외 처리 =====

    /**
     * @Valid 검증 실패 예외 처리 (Request Body)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed for request body: {}", ex.getMessage());
        
        List<String> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(String.format("%s: %s", error.getField(), error.getDefaultMessage()));
        }
        
        ApiResponse<List<String>> response = ApiResponse.error("입력값 검증에 실패했습니다.", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 제약조건 위반 예외 처리 (Path Variable, Request Parameter)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        List<String> errors = new ArrayList<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errors.add(String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()));
        }
        
        ApiResponse<List<String>> response = ApiResponse.error("제약조건 위반이 발생했습니다.", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===== HTTP 요청 관련 예외 처리 =====

    /**
     * 필수 요청 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getMessage());
        
        String message = String.format("필수 파라미터가 누락되었습니다: %s", ex.getParameterName());
        ApiResponse<Object> response = ApiResponse.badRequest(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 메서드 파라미터 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getMessage());
        
        String message = String.format("파라미터 타입이 올바르지 않습니다: %s", ex.getName());
        ApiResponse<Object> response = ApiResponse.badRequest(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * HTTP 메시지 읽기 불가 예외 처리 (잘못된 JSON 형식 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.badRequest("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 지원하지 않는 미디어 타입 예외 처리
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        log.warn("HTTP media type not supported: {}", ex.getMessage());
        
        String message = String.format("지원하지 않는 미디어 타입입니다: %s", ex.getContentType());
        ApiResponse<Object> response = ApiResponse.badRequest(message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    // ===== JWT 관련 예외 처리 =====

    /**
     * JWT 예외 처리 (기본)
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtException(JwtException ex) {
        log.warn("JWT exception: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * JWT 만료 예외 처리
     */
    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtExpiredException(JwtExpiredException ex) {
        log.warn("JWT expired: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * JWT 유효하지 않음 예외 처리
     */
    @ExceptionHandler(JwtInvalidException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtInvalidException(JwtInvalidException ex) {
        log.warn("Invalid JWT: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
    
    /**
     * JJWT 라이브러리 예외 처리 (고급 JWT 예외 처리)
     */
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJjwtException(io.jsonwebtoken.JwtException ex) {
        log.warn("JJWT library exception: {}", ex.getMessage());
        
        String message = "토큰 처리 중 오류가 발생했습니다.";
        
        if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            message = "토큰이 만료되었습니다. 다시 로그인해 주세요.";
        } else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
            message = "잘못된 형식의 토큰입니다.";
        } else if (ex instanceof io.jsonwebtoken.UnsupportedJwtException) {
            message = "지원하지 않는 토큰 형식입니다.";
        } else if (ex instanceof io.jsonwebtoken.security.SignatureException) {
            message = "토큰 서명이 유효하지 않습니다.";
        } else if (ex instanceof io.jsonwebtoken.PrematureJwtException) {
            message = "토큰이 아직 유효하지 않습니다.";
        }
        
        ApiResponse<Object> response = ApiResponse.error(message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 지원하지 않는 HTTP 메서드 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        
        String message = String.format("지원하지 않는 HTTP 메서드입니다: %s", ex.getMethod());
        ApiResponse<Object> response = ApiResponse.badRequest(message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 리소스를 찾을 수 없음 예외 처리 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.notFound("요청한 리소스를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ===== 일반 예외 처리 =====

    /**
     * IllegalArgument 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * IllegalState 예외 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.badRequest("시스템 상태가 올바르지 않습니다: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 런타임 예외 처리 (기타 모든 RuntimeException)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime exception occurred", ex);
        
        ApiResponse<Object> response = ApiResponse.internalServerError("서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 모든 예외의 최종 처리기 (예상치 못한 모든 예외)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        
        ApiResponse<Object> response = ApiResponse.internalServerError("서버에서 예상치 못한 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}