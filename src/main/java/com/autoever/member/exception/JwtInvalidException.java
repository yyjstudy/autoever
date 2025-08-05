package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT 토큰 유효하지 않음 예외
 * JWT 토큰 형식이 잘못되었거나 서명이 유효하지 않을 때 발생
 */
public class JwtInvalidException extends BaseCustomException {
    
    private static final String ERROR_CODE = "JWT_INVALID";
    private static final String DEFAULT_MESSAGE = "유효하지 않은 JWT 토큰입니다.";
    
    public JwtInvalidException() {
        super(DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
    
    public JwtInvalidException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
}