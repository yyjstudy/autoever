package com.autoever.member.dto;

import com.autoever.member.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 * 회원가입 시 필요한 모든 정보와 유효성 검증 규칙을 포함
 */
@PasswordMatches
public record UserRegistrationDto(
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 밑줄(_)만 사용 가능합니다")
    String username,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다")
    String password,
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    String confirmPassword,
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 100, message = "이름은 2-100자 사이여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문, 공백만 사용 가능합니다")
    String name,
    
    @NotBlank(message = "주민등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{6}-\\d{7}$", message = "주민등록번호 형식이 올바르지 않습니다 (XXXXXX-XXXXXXX)")
    String socialNumber,
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    String email,
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (XXX-XXXX-XXXX)")
    String phoneNumber,
    
    @NotBlank(message = "주소는 필수입니다")
    @Size(min = 5, max = 500, message = "주소는 5-500자 사이여야 합니다")
    String address
) {}