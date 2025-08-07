package com.autoever.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 테스트용 유저 생성 요청 DTO
 */
@Schema(description = "테스트용 유저 생성 요청")
public record TestUserCreateRequest(
    
    @NotNull(message = "생성할 유저 수는 필수입니다")
    @Min(value = 1, message = "생성할 유저 수는 최소 1명 이상이어야 합니다")
    @Max(value = 100, message = "생성할 유저 수는 최대 100명까지 가능합니다")
    @Schema(description = "생성할 유저 수", example = "10", required = true)
    Integer userCount,
    
    @Min(value = 10, message = "연령대는 10 이상이어야 합니다")
    @Max(value = 90, message = "연령대는 90 이하여야 합니다")
    @Schema(description = "연령대 (10, 20, 30, ..., 90). 입력하지 않으면 0~99세 랜덤", example = "30", required = false)
    Integer ageGroup
) {
}