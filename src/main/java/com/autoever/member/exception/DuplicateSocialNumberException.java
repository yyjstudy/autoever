package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 주민등록번호 중복 예외
 * 이미 등록된 주민등록번호로 회원가입을 시도할 때 발생
 */
public class DuplicateSocialNumberException extends BaseCustomException {
    
    private static final String ERROR_CODE = "DUPLICATE_SOCIAL_NUMBER";
    private static final HttpStatus HTTP_STATUS = HttpStatus.CONFLICT; // 409
    
    public DuplicateSocialNumberException() {
        super(
            "이미 등록된 주민등록번호입니다.",
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public DuplicateSocialNumberException(String message) {
        super(
            message,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public DuplicateSocialNumberException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
}