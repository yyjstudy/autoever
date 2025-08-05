package com.autoever.member.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class BaseCustomExceptionTest {

    @Test
    @DisplayName("BaseCustomException 기본 생성자 테스트")
    void baseCustomExceptionConstructorTest() {
        // given
        String message = "테스트 예외 메시지";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        String errorCode = "TEST_ERROR";
        
        // when
        TestCustomException exception = new TestCustomException(message, httpStatus, errorCode);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getStatusValue()).isEqualTo(400);
    }

    @Test
    @DisplayName("BaseCustomException cause와 함께 생성자 테스트")
    void baseCustomExceptionWithCauseTest() {
        // given
        String message = "테스트 예외 메시지";
        Throwable cause = new RuntimeException("원인 예외");
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "INTERNAL_ERROR";
        
        // when
        TestCustomException exception = new TestCustomException(message, cause, httpStatus, errorCode);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getStatusValue()).isEqualTo(500);
    }

    @Test
    @DisplayName("BaseCustomException toString 메서드 테스트")
    void toStringTest() {
        // given
        String message = "테스트 메시지";
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        String errorCode = "NOT_FOUND_ERROR";
        
        // when
        TestCustomException exception = new TestCustomException(message, httpStatus, errorCode);
        String toStringResult = exception.toString();
        
        // then
        assertThat(toStringResult).contains("TestCustomException");
        assertThat(toStringResult).contains("httpStatus=404 NOT_FOUND");
        assertThat(toStringResult).contains("errorCode='NOT_FOUND_ERROR'");
        assertThat(toStringResult).contains("message='테스트 메시지'");
    }

    @Test  
    @DisplayName("BaseCustomException 상속 테스트")
    void inheritanceTest() {
        // given
        TestCustomException exception = new TestCustomException(
            "테스트", HttpStatus.BAD_REQUEST, "TEST"
        );
        
        // when & then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(BaseCustomException.class);
    }

    // 테스트용 구체적인 예외 클래스
    private static class TestCustomException extends BaseCustomException {
        public TestCustomException(String message, HttpStatus httpStatus, String errorCode) {
            super(message, httpStatus, errorCode);
        }
        
        public TestCustomException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
            super(message, cause, httpStatus, errorCode);
        }
    }
}