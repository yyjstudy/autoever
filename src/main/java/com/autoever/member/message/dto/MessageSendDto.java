package com.autoever.member.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 대량 메시지 발송 요청 DTO
 */
public record MessageSendDto(
    @Schema(description = "연령대", example = "TWENTIES", 
            allowableValues = {"TEENS", "TWENTIES", "THIRTIES", "FORTIES", "FIFTIES_PLUS"})
    @NotNull(message = "연령대는 필수입니다")
    @Pattern(regexp = "TEENS|TWENTIES|THIRTIES|FORTIES|FIFTIES_PLUS", 
             message = "유효한 연령대를 선택해주세요")
    String ageGroup,
    
    @Schema(description = "메시지 내용 (자동으로 '{회원 성명}님, 안녕하세요. 현대 오토에버입니다.' 템플릿이 앞에 추가됩니다)", 
            example = "신제품 출시 기념 20% 할인 혜택을 놓치지 마세요!")
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하로 작성해주세요")
    String message
) {
    
    /**
     * 연령대 열거형 반환
     */
    @Schema(hidden = true)
    public AgeGroup getAgeGroupEnum() {
        try {
            return AgeGroup.valueOf(ageGroup);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}