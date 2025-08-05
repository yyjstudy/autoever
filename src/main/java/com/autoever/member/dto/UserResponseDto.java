package com.autoever.member.dto;

import com.autoever.member.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UserResponseDto {
    
    private Long id;
    private String username;
    private String name;
    private String socialNumber;
    private String phoneNumber;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.socialNumber = user.getMaskedSocialNumber();
        this.phoneNumber = user.getMaskedPhoneNumber();
        this.address = user.getAddress();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
    
    public static UserResponseDto from(User user) {
        return new UserResponseDto(user);
    }
}