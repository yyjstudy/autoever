package com.autoever.member.message.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageSendDto 테스트")
class MessageSendDtoTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
    
    @Test
    @DisplayName("유효한 요청 데이터")
    void validRequest() {
        MessageSendDto dto = new MessageSendDto("TWENTIES", "할인 쿠폰이 발급되었습니다!");
        
        Set<ConstraintViolation<MessageSendDto>> violations = validator.validate(dto);
        
        assertThat(violations).isEmpty();
        assertThat(dto.getAgeGroupEnum()).isEqualTo(AgeGroup.TWENTIES);
    }
    
    @Test
    @DisplayName("연령대 필수 검증")
    void ageGroupRequired() {
        MessageSendDto dto = new MessageSendDto(null, "메시지");
        
        Set<ConstraintViolation<MessageSendDto>> violations = validator.validate(dto);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("연령대는 필수입니다");
    }
    
    @Test
    @DisplayName("유효하지 않은 연령대")
    void invalidAgeGroup() {
        MessageSendDto dto = new MessageSendDto("INVALID", "메시지");
        
        Set<ConstraintViolation<MessageSendDto>> violations = validator.validate(dto);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("유효한 연령대를 선택해주세요");
    }
    
    @Test
    @DisplayName("메시지 필수 검증")
    void messageRequired() {
        MessageSendDto dto = new MessageSendDto("TEENS", "");
        
        Set<ConstraintViolation<MessageSendDto>> violations = validator.validate(dto);
        
        assertThat(violations).hasSize(2); // @NotBlank와 @Size 둘 다 위반
        assertThat(violations).anyMatch(v -> v.getMessage().equals("메시지 내용은 필수입니다"));
        assertThat(violations).anyMatch(v -> v.getMessage().equals("메시지는 1자 이상 1000자 이하로 작성해주세요"));
    }
    
    @Test
    @DisplayName("메시지 길이 제한")
    void messageLengthLimit() {
        String longMessage = "a".repeat(1001);
        MessageSendDto dto = new MessageSendDto("THIRTIES", longMessage);
        
        Set<ConstraintViolation<MessageSendDto>> violations = validator.validate(dto);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("메시지는 1자 이상 1000자 이하로 작성해주세요");
    }
}