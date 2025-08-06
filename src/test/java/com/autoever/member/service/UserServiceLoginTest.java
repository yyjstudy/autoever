package com.autoever.member.service;

import com.autoever.member.dto.JwtTokenDto;
import com.autoever.member.dto.LoginDto;
import com.autoever.member.exception.InvalidCredentialsException;
import com.autoever.member.jwt.JwtUtil;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UserService 로그인 기능 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceLoginTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private UserService userService;
    
    private LoginDto validLoginDto;
    private String mockJwtToken;
    private long mockExpiresIn;
    
    @BeforeEach
    void setUp() {
        validLoginDto = new LoginDto("testuser", "Password123!");
        mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock.token";
        mockExpiresIn = 86400000L; // 24시간
    }
    
    @Test
    @DisplayName("유효한 자격증명으로 로그인 성공")
    void login_ValidCredentials_Success() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(validLoginDto.username());
        when(jwtUtil.generateToken(validLoginDto.username())).thenReturn(mockJwtToken);
        when(jwtUtil.getExpirationTime()).thenReturn(mockExpiresIn);
        
        // When
        JwtTokenDto result = userService.login(validLoginDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(mockJwtToken);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(mockExpiresIn / 1000); // 밀리초를 초로 변환
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(validLoginDto.username());
        verify(jwtUtil).getExpirationTime();
    }
    
    @Test
    @DisplayName("잘못된 사용자명으로 로그인 실패")
    void login_InvalidUsername_ThrowsException() {
        // Given
        LoginDto invalidLoginDto = new LoginDto("wronguser", "Password123!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // When & Then
        assertThatThrownBy(() -> userService.login(invalidLoginDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("사용자명 또는 비밀번호가 올바르지 않습니다.");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void login_InvalidPassword_ThrowsException() {
        // Given
        LoginDto invalidLoginDto = new LoginDto("testuser", "WrongPassword!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // When & Then
        assertThatThrownBy(() -> userService.login(invalidLoginDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("사용자명 또는 비밀번호가 올바르지 않습니다.");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }
    
    @Test
    @DisplayName("AuthenticationManager 인증 호출 검증")
    void login_VerifyAuthenticationManagerCall() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(validLoginDto.username());
        when(jwtUtil.generateToken(anyString())).thenReturn(mockJwtToken);
        when(jwtUtil.getExpirationTime()).thenReturn(mockExpiresIn);
        
        // When
        userService.login(validLoginDto);
        
        // Then
        verify(authenticationManager).authenticate(argThat(authToken -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authToken;
            return token.getPrincipal().equals(validLoginDto.username()) &&
                   token.getCredentials().equals(validLoginDto.password());
        }));
    }
    
    @Test
    @DisplayName("JWT 토큰 생성 시 사용자명 전달 검증")
    void login_VerifyJwtGenerationWithUsername() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(validLoginDto.username());
        when(jwtUtil.generateToken(validLoginDto.username())).thenReturn(mockJwtToken);
        when(jwtUtil.getExpirationTime()).thenReturn(mockExpiresIn);
        
        // When
        userService.login(validLoginDto);
        
        // Then
        verify(jwtUtil).generateToken(validLoginDto.username());
    }
}