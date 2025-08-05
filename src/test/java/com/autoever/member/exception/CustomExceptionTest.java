package com.autoever.member.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class CustomExceptionTest {

    @Test
    @DisplayName("DuplicateAccountException 생성 및 속성 테스트")
    void duplicateAccountExceptionTest() {
        // given
        String username = "testuser";
        
        // when
        DuplicateAccountException exception = new DuplicateAccountException(username);
        
        // then
        assertThat(exception.getMessage()).isEqualTo("이미 존재하는 사용자명입니다: testuser");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getErrorCode()).isEqualTo("DUPLICATE_ACCOUNT");
        assertThat(exception.getStatusValue()).isEqualTo(409);
    }

    @Test
    @DisplayName("DuplicateAccountException cause와 함께 생성 테스트")
    void duplicateAccountExceptionWithCauseTest() {
        // given
        String username = "testuser";
        Throwable cause = new RuntimeException("DB 제약조건 위반");
        
        // when
        DuplicateAccountException exception = new DuplicateAccountException(username, cause);
        
        // then
        assertThat(exception.getMessage()).contains("testuser");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("DuplicateSocialNumberException 기본 생성자 테스트")
    void duplicateSocialNumberExceptionDefaultTest() {
        // when
        DuplicateSocialNumberException exception = new DuplicateSocialNumberException();
        
        // then
        assertThat(exception.getMessage()).isEqualTo("이미 등록된 주민등록번호입니다.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getErrorCode()).isEqualTo("DUPLICATE_SOCIAL_NUMBER");
        assertThat(exception.getStatusValue()).isEqualTo(409);
    }

    @Test
    @DisplayName("DuplicateSocialNumberException 커스텀 메시지 테스트")
    void duplicateSocialNumberExceptionCustomMessageTest() {
        // given
        String customMessage = "주민등록번호가 이미 사용 중입니다.";
        
        // when
        DuplicateSocialNumberException exception = new DuplicateSocialNumberException(customMessage);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(customMessage);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("UserNotFoundException ID로 생성 테스트")
    void userNotFoundExceptionByIdTest() {
        // given
        Long userId = 123L;
        
        // when
        UserNotFoundException exception = new UserNotFoundException(userId);
        
        // then
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다. ID: 123");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(exception.getStatusValue()).isEqualTo(404);
    }

    @Test
    @DisplayName("UserNotFoundException 사용자명으로 생성 테스트")
    void userNotFoundExceptionByUsernameTest() {
        // given
        String username = "nonexistentuser";
        
        // when
        UserNotFoundException exception = new UserNotFoundException(username);
        
        // then
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다. 사용자명: nonexistentuser");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UserNotFoundException 정적 팩토리 메서드 테스트")
    void userNotFoundExceptionStaticFactoryTest() {
        // when - byId
        UserNotFoundException byIdException = UserNotFoundException.byId(456L);
        
        // then
        assertThat(byIdException.getMessage()).contains("ID: 456");
        
        // when - byUsername
        UserNotFoundException byUsernameException = UserNotFoundException.byUsername("testuser");
        
        // then
        assertThat(byUsernameException.getMessage()).contains("사용자명: testuser");
    }

    @Test
    @DisplayName("InvalidCredentialsException 기본 생성자 테스트")
    void invalidCredentialsExceptionDefaultTest() {
        // when
        InvalidCredentialsException exception = new InvalidCredentialsException();
        
        // then
        assertThat(exception.getMessage()).isEqualTo("사용자명 또는 비밀번호가 올바르지 않습니다.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(exception.getStatusValue()).isEqualTo(401);
    }

    @Test
    @DisplayName("InvalidCredentialsException 커스텀 메시지 테스트")
    void invalidCredentialsExceptionCustomMessageTest() {
        // given
        String customMessage = "로그인 정보가 일치하지 않습니다.";
        
        // when
        InvalidCredentialsException exception = new InvalidCredentialsException(customMessage);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(customMessage);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AccessDeniedException 기본 생성자 테스트")
    void accessDeniedExceptionDefaultTest() {
        // when
        AccessDeniedException exception = new AccessDeniedException();
        
        // then
        assertThat(exception.getMessage()).isEqualTo("해당 리소스에 대한 접근 권한이 없습니다.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(exception.getStatusValue()).isEqualTo(403);
    }

    @Test
    @DisplayName("AccessDeniedException 리소스 지정 생성자 테스트")
    void accessDeniedExceptionWithResourceTest() {
        // given
        String resource = "관리자 페이지";
        
        // when
        AccessDeniedException exception = new AccessDeniedException(resource);
        
        // then
        assertThat(exception.getMessage()).isEqualTo("'관리자 페이지' 리소스에 대한 접근 권한이 없습니다.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("BusinessLogicException 기본 테스트")
    void businessLogicExceptionTest() {
        // given
        String message = "비즈니스 규칙 위반";
        
        // when
        BusinessLogicException exception = new BusinessLogicException(message);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo("BUSINESS_LOGIC_ERROR");
        assertThat(exception.getStatusValue()).isEqualTo(400);
    }

    @Test
    @DisplayName("BusinessLogicException 커스텀 에러 코드 테스트")
    void businessLogicExceptionCustomErrorCodeTest() {
        // given
        String message = "계정 상태 오류";
        String customErrorCode = "ACCOUNT_STATUS_ERROR";
        
        // when
        BusinessLogicException exception = new BusinessLogicException(message, customErrorCode);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo(customErrorCode);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("InvalidRequestException 기본 테스트")
    void invalidRequestExceptionTest() {
        // given
        String message = "잘못된 요청입니다.";
        
        // when
        InvalidRequestException exception = new InvalidRequestException(message);
        
        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(exception.getStatusValue()).isEqualTo(400);
    }

    @Test
    @DisplayName("InvalidRequestException 정적 팩토리 메서드 테스트")
    void invalidRequestExceptionStaticFactoryTest() {
        // when - missingParameter
        InvalidRequestException missingException = InvalidRequestException.missingParameter("username");
        
        // then
        assertThat(missingException.getMessage()).isEqualTo("필수 파라미터가 누락되었습니다: username");
        
        // when - invalidParameter
        InvalidRequestException invalidException = InvalidRequestException.invalidParameter("age", "음수 값은 허용되지 않습니다");
        
        // then
        assertThat(invalidException.getMessage()).isEqualTo("잘못된 파라미터입니다. age: 음수 값은 허용되지 않습니다");
    }

    @Test
    @DisplayName("모든 예외가 BaseCustomException을 상속하는지 테스트")
    void inheritanceTest() {
        // given & when & then
        assertThat(new DuplicateAccountException("test")).isInstanceOf(BaseCustomException.class);
        assertThat(new DuplicateSocialNumberException()).isInstanceOf(BaseCustomException.class);
        assertThat(new UserNotFoundException(1L)).isInstanceOf(BaseCustomException.class);
        assertThat(new InvalidCredentialsException()).isInstanceOf(BaseCustomException.class);
        assertThat(new AccessDeniedException()).isInstanceOf(BaseCustomException.class);
        assertThat(new BusinessLogicException("test")).isInstanceOf(BaseCustomException.class);
        assertThat(new InvalidRequestException("test")).isInstanceOf(BaseCustomException.class);
    }

    @Test
    @DisplayName("HTTP 상태 코드 매핑 검증 테스트")
    void httpStatusMappingTest() {
        // Conflict (409) 예외들
        assertThat(new DuplicateAccountException("test").getStatusValue()).isEqualTo(409);
        assertThat(new DuplicateSocialNumberException().getStatusValue()).isEqualTo(409);
        
        // Not Found (404) 예외
        assertThat(new UserNotFoundException(1L).getStatusValue()).isEqualTo(404);
        
        // Unauthorized (401) 예외
        assertThat(new InvalidCredentialsException().getStatusValue()).isEqualTo(401);
        
        // Forbidden (403) 예외
        assertThat(new AccessDeniedException().getStatusValue()).isEqualTo(403);
        
        // Bad Request (400) 예외들
        assertThat(new BusinessLogicException("test").getStatusValue()).isEqualTo(400);
        assertThat(new InvalidRequestException("test").getStatusValue()).isEqualTo(400);
    }
}