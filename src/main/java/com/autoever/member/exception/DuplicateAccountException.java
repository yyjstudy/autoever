package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 사용자명 중복 예외
 * 이미 존재하는 사용자명으로 회원가입을 시도할 때 발생
 */
public class DuplicateAccountException extends BaseCustomException {
    
    private static final String ERROR_CODE = "DUPLICATE_ACCOUNT";
    private static final HttpStatus HTTP_STATUS = HttpStatus.CONFLICT; // 409
    
    public DuplicateAccountException(String username) {
        super(
            String.format("이미 존재하는 사용자명입니다: %s", username),
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public DuplicateAccountException(String username, Throwable cause) {
        super(
            String.format("이미 존재하는 사용자명입니다: %s", username),
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
}