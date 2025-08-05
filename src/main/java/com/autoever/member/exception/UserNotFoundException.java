package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 사용자 찾을 수 없음 예외
 * 요청된 사용자가 시스템에 존재하지 않을 때 발생
 */
public class UserNotFoundException extends BaseCustomException {
    
    private static final String ERROR_CODE = "USER_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND; // 404
    
    public UserNotFoundException(Long userId) {
        super(
            String.format("사용자를 찾을 수 없습니다. ID: %d", userId),
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public UserNotFoundException(String username) {
        super(
            String.format("사용자를 찾을 수 없습니다. 사용자명: %s", username),
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public static UserNotFoundException byId(Long userId) {
        return new UserNotFoundException(userId);
    }
    
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException(username);
    }
}