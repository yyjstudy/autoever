package com.autoever.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDto {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (XXX-XXXX-XXXX)")
    private String phoneNumber;
    
    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
    private String address;
}