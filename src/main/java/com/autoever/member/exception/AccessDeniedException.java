package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 권한 없음 예외
 * 요청된 리소스에 대한 접근 권한이 없을 때 발생
 */
public class AccessDeniedException extends BaseCustomException {
    
    private static final String ERROR_CODE = "ACCESS_DENIED";
    private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN; // 403
    
    public AccessDeniedException() {
        super(
            "해당 리소스에 대한 접근 권한이 없습니다.",
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public AccessDeniedException(String resource) {
        super(
            String.format("'%s' 리소스에 대한 접근 권한이 없습니다.", resource),
            HTTP_STATUS,
            ERROR_CODE
        );
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(
            message,
            cause,
            HTTP_STATUS,
            ERROR_CODE
        );
    }
}