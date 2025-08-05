package com.autoever.member.validation;

import com.autoever.member.dto.UserRegistrationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 비밀번호 일치 검증을 수행하는 Validator
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserRegistrationDto> {
    
    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        // 초기화 로직이 필요한 경우 여기에 구현
    }
    
    @Override
    public boolean isValid(UserRegistrationDto userRegistrationDto, ConstraintValidatorContext context) {
        if (userRegistrationDto == null) {
            return true; // null 체크는 다른 validation에서 처리
        }
        
        String password = userRegistrationDto.password();
        String confirmPassword = userRegistrationDto.confirmPassword();
        
        // 둘 다 null이거나 값이 일치하면 유효
        if (password == null && confirmPassword == null) {
            return true;
        }
        
        if (password == null || confirmPassword == null) {
            return false;
        }
        
        return password.equals(confirmPassword);
    }
}