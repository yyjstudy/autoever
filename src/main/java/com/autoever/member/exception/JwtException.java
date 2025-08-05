package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT 관련 예외
 * JWT 토큰 처리 중 발생하는 모든 예외의 기본 클래스
 */
public class JwtException extends BaseCustomException {
    
    private static final String ERROR_CODE = "JWT_ERROR";
    
    public JwtException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
    
    public JwtException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
}