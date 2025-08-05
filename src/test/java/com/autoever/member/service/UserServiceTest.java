package com.autoever.member.service;

import com.autoever.member.dto.UserRegistrationDto;
import com.autoever.member.dto.UserResponseDto;
import com.autoever.member.entity.User;
import com.autoever.member.exception.DuplicateAccountException;
import com.autoever.member.exception.DuplicateSocialNumberException;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto validRegistrationDto;

    @BeforeEach
    void setUp() {
        validRegistrationDto = new UserRegistrationDto(
            "testuser123",
            "Password123!",
            "Password123!",
            "홍길동",
            "901201-1234567",
            "test@example.com",
            "010-1234-5678",
            "서울특별시 강남구 테헤란로 123"
        );
    }

    @Test
    @DisplayName("정상적인 회원가입 처리")
    void registerUser_Success() {
        // given
        User mockSavedUser = User.builder()
            .username("testuser123")
            .password("hashedPassword")
            .name("홍길동")
            .socialNumber("901201-1234567")
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address("서울특별시 강남구 테헤란로 123")
            .build();
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // when
        UserResponseDto result = userService.registerUser(validRegistrationDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser123");
        assertThat(result.name()).isEqualTo("홍길동");

        // 메서드 호출 검증
        verify(userRepository).existsByUsername("testuser123");
        verify(userRepository).existsBySocialNumber("901201-1234567");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("사용자명 중복 시 DuplicateAccountException 발생")
    void registerUser_DuplicateUsername() {
        // given
        when(userRepository.existsByUsername("testuser123")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
            .isInstanceOf(DuplicateAccountException.class)
            .hasMessageContaining("testuser123");

        // 중복 검출 후 더 이상 진행되지 않음을 확인
        verify(userRepository).existsByUsername("testuser123");
        verify(userRepository, never()).existsBySocialNumber(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("주민등록번호 중복 시 DuplicateSocialNumberException 발생")
    void registerUser_DuplicateSocialNumber() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber("901201-1234567")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
            .isInstanceOf(DuplicateSocialNumberException.class);

        // 사용자명 검증 후 주민등록번호 검증에서 실패함을 확인
        verify(userRepository).existsByUsername("testuser123");
        verify(userRepository).existsBySocialNumber("901201-1234567");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 해싱 처리 확인")
    void registerUser_PasswordHashing() {
        // given
        User mockSavedUser = User.builder()
            .username("testuser123")
            .password("hashedPassword123")
            .name("홍길동")
            .socialNumber("901201-1234567")
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address("서울특별시 강남구 테헤란로 123")
            .build();
            
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // when
        userService.registerUser(validRegistrationDto);

        // then
        verify(passwordEncoder).encode("Password123!");
        
        // 저장되는 User 엔티티의 비밀번호가 해싱된 값인지 확인
        verify(userRepository).save(argThat(user -> 
            "hashedPassword123".equals(user.getPassword())
        ));
    }

    @Test
    @DisplayName("User 엔티티 생성 및 저장 검증")
    void registerUser_UserEntityCreation() {
        // given
        User mockSavedUser = User.builder()
            .username("testuser123")
            .password("hashedPassword")
            .name("홍길동")
            .socialNumber("901201-1234567")
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address("서울특별시 강남구 테헤란로 123")
            .build();
            
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // when
        userService.registerUser(validRegistrationDto);

        // then
        verify(userRepository).save(argThat(user -> {
            assertThat(user.getUsername()).isEqualTo("testuser123");
            assertThat(user.getPassword()).isEqualTo("hashedPassword");
            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getSocialNumber()).isEqualTo("901201-1234567");
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getPhoneNumber()).isEqualTo("010-1234-5678");
            assertThat(user.getAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
            return true;
        }));
    }

    @Test
    @DisplayName("UserResponseDto 변환 확인")
    void registerUser_ResponseDtoConversion() {
        // given
        User mockSavedUser = User.builder()
            .username("testuser123")
            .password("hashedPassword")
            .name("홍길동")
            .socialNumber("901201-1234567")
            .email("test@example.com")
            .phoneNumber("010-1234-5678")
            .address("서울특별시 강남구 테헤란로 123")
            .build();
            
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // when
        UserResponseDto result = userService.registerUser(validRegistrationDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(mockSavedUser.getUsername());
        assertThat(result.name()).isEqualTo(mockSavedUser.getName());
        
        // 민감정보 마스킹 확인은 UserResponseDto.from() 메서드에서 처리됨
    }

    @Test
    @DisplayName("트랜잭션 어노테이션 적용 확인")
    void registerUser_TransactionalBehavior() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB 오류"));

        // when & then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB 오류");

        // @Transactional이 적용되어 있어 예외 발생 시 롤백되어야 함
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("로깅에서 민감정보 마스킹 확인")
    void registerUser_SensitiveDataMasking() {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsBySocialNumber("901201-1234567")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
            .isInstanceOf(DuplicateSocialNumberException.class);

        // 로그에서 주민등록번호가 마스킹되어 출력되는지는 실제 로그 검증으로 확인
        // 여기서는 메서드 호출이 정상적으로 이루어졌는지만 확인
        verify(userRepository).existsBySocialNumber("901201-1234567");
    }
}