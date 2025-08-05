package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 일반적인 비즈니스 로직 예외
 * 특정 비즈니스 규칙 위반 시 발생하는 범용 예외
 */
public class BusinessLogicException extends BaseCustomException {
    
    private static final String ERROR_CODE = "BUSINESS_LOGIC_ERROR";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST; // 400
    
    public BusinessLogicException(String message) {
        super(
            message,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public BusinessLogicException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public BusinessLogicException(String message, String customErrorCode) {
        super(
            message,
            HTTP_STATUS,
            customErrorCode
        );
    }
    
    public BusinessLogicException(String message, Throwable cause, String customErrorCode) {
        super(
            message,
            cause,
            HTTP_STATUS,
            customErrorCode
        );
    }
}