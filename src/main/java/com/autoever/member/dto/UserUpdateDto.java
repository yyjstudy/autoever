package com.autoever.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정 요청 DTO
 * - 암호만 수정
 * - 주소만 수정  
 * - 암호와 주소 동시 수정
 * null 값은 해당 필드를 업데이트하지 않음을 의미합니다.
 */
@Schema(description = "회원 정보 수정 요청 DTO - 주소와 비밀번호만 수정 가능")
public record UserUpdateDto(
    @Schema(description = "변경할 주소 (null인 경우 변경하지 않음)", example = "서울특별시 강남구 테헤란로 123", nullable = true)
    @Size(min = 5, max = 500, message = "주소는 5-500자 사이여야 합니다")
    String address,
    
    @Schema(description = "변경할 비밀번호 (null인 경우 변경하지 않음)", example = "NewPassword123!", nullable = true)
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다")
    String password
) {
    /**
     * 주소 업데이트 여부 확인
     */
    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }
    
    /**
     * 비밀번호 업데이트 여부 확인
     */
    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }
    
    /**
     * 업데이트할 필드가 있는지 확인
     */
    public boolean hasAnyUpdate() {
        return hasAddress() || hasPassword();
    }
}