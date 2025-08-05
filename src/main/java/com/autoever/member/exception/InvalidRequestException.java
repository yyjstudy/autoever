package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 잘못된 요청 예외
 * 클라이언트 요청이 잘못되었거나 유효하지 않을 때 발생
 */
public class InvalidRequestException extends BaseCustomException {
    
    private static final String ERROR_CODE = "INVALID_REQUEST";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST; // 400
    
    public InvalidRequestException(String message) {
        super(
            message,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public static InvalidRequestException missingParameter(String parameterName) {
        return new InvalidRequestException(
            String.format("필수 파라미터가 누락되었습니다: %s", parameterName)
        );
    }
    
    public static InvalidRequestException invalidParameter(String parameterName, String reason) {
        return new InvalidRequestException(
            String.format("잘못된 파라미터입니다. %s: %s", parameterName, reason)
        );
    }
}