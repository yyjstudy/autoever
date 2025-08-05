package com.autoever.member.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 구조
 * 모든 REST API 응답에 사용되는 표준화된 응답 형식
 * 
 * @param <T> 응답 데이터의 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp
) {
    
    // 성공 응답 생성 메서드들
    
    /**
     * 데이터와 함께 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data, LocalDateTime.now());
    }
    
    /**
     * 메시지와 데이터가 함께 있는 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
    
    /**
     * 데이터 없이 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }
    
    /**
     * 생성 성공 응답 (HTTP 201)
     */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "리소스가 성공적으로 생성되었습니다.", data, LocalDateTime.now());
    }
    
    /**
     * 생성 성공 응답 with custom message (HTTP 201)
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
    
    // 오류 응답 생성 메서드들
    
    /**
     * 오류 응답 생성
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
    
    /**
     * 오류 응답 생성 with data
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now());
    }
    
    /**
     * Bad Request 오류 응답 (HTTP 400)
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
    
    /**
     * 인증 실패 응답 (HTTP 401)
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, message != null ? message : "인증이 필요합니다.", null, LocalDateTime.now());
    }
    
    /**
     * 권한 없음 응답 (HTTP 403)
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, message != null ? message : "접근 권한이 없습니다.", null, LocalDateTime.now());
    }
    
    /**
     * 리소스 찾을 수 없음 응답 (HTTP 404)
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, message != null ? message : "요청한 리소스를 찾을 수 없습니다.", null, LocalDateTime.now());
    }
    
    /**
     * 충돌 오류 응답 (HTTP 409)
     */
    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
    
    /**
     * 서버 내부 오류 응답 (HTTP 500)
     */
    public static <T> ApiResponse<T> internalServerError(String message) {
        return new ApiResponse<>(false, message != null ? message : "서버 내부 오류가 발생했습니다.", null, LocalDateTime.now());
    }
}