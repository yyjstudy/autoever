package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT 토큰 만료 예외
 * JWT 토큰이 만료되었을 때 발생
 */
public class JwtExpiredException extends BaseCustomException {
    
    private static final String ERROR_CODE = "JWT_EXPIRED";
    private static final String DEFAULT_MESSAGE = "JWT 토큰이 만료되었습니다.";
    
    public JwtExpiredException() {
        super(DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
    
    public JwtExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
}