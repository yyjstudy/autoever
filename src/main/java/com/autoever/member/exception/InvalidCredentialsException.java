package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 실패 예외
 * 잘못된 사용자명 또는 비밀번호로 로그인을 시도할 때 발생
 */
public class InvalidCredentialsException extends BaseCustomException {
    
    private static final String ERROR_CODE = "INVALID_CREDENTIALS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED; // 401
    
    public InvalidCredentialsException() {
        super(
            "사용자명 또는 비밀번호가 올바르지 않습니다.",
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public InvalidCredentialsException(String message) {
        super(
            message,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
}