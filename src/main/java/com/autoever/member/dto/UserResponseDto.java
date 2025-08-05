package com.autoever.member.dto;

import com.autoever.member.entity.User;

import java.time.LocalDateTime;

public record UserResponseDto(
    Long id,
    String username,
    String name,
    String socialNumber,
    String email,
    String phoneNumber,
    String address,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getMaskedSocialNumber(),
            user.getEmail(),
            user.getMaskedPhoneNumber(),
            user.getAddress(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}