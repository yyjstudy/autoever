package com.autoever.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 요청 DTO
 */
@Schema(description = "로그인 요청 DTO")
public record LoginDto(
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    @Schema(description = "사용자명", example = "testuser", minLength = 3, maxLength = 50)
    String username,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    @Schema(description = "비밀번호", example = "Test1234!", minLength = 8, maxLength = 100)
    String password
) {
}