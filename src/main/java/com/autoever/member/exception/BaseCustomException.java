package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 커스텀 예외의 기본 클래스
 * HTTP 상태 코드와 에러 메시지를 포함하여 일관된 예외 처리 구조 제공
 */
public abstract class BaseCustomException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    
    /**
     * 기본 생성자
     * 
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     * @param errorCode 에러 코드
     */
    protected BaseCustomException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    /**
     * 원인 예외와 함께 생성하는 생성자
     * 
     * @param message 에러 메시지
     * @param cause 원인 예외
     * @param httpStatus HTTP 상태 코드  
     * @param errorCode 에러 코드
     */
    protected BaseCustomException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    /**
     * HTTP 상태 코드 반환
     * 
     * @return HTTP 상태 코드
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * 에러 코드 반환
     * 
     * @return 에러 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * HTTP 상태 코드 값 반환 (정수형)
     * 
     * @return HTTP 상태 코드 값
     */
    public int getStatusValue() {
        return httpStatus.value();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "httpStatus=" + httpStatus +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}