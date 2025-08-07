package com.autoever.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 테스트용 유저 생성 응답 DTO
 */
@Schema(description = "테스트용 유저 생성 응답")
public record TestUserCreateResponse(
    
    @Schema(description = "생성된 유저 수", example = "10")
    int createdCount,
    
    @Schema(description = "생성된 유저 ID 목록")
    List<Long> userIds,
    
    @Schema(description = "생성된 유저 정보 요약")
    List<UserSummary> userSummaries
) {
    
    @Schema(description = "생성된 유저 정보 요약")
    public record UserSummary(
        @Schema(description = "유저 ID")
        Long id,
        @Schema(description = "사용자명")
        String username,
        @Schema(description = "이름")
        String name,
        @Schema(description = "전화번호")
        String phoneNumber,
        @Schema(description = "이메일")
        String email,
        @Schema(description = "계산된 나이")
        int age
    ) {}
}