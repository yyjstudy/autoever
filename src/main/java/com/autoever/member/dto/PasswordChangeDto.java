package com.autoever.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeDto(
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    String currentPassword,
    
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다")
    String newPassword,
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    String confirmPassword
) {}