package com.autoever.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "UK_users_username", columnNames = "username"),
    @UniqueConstraint(name = "UK_users_social_number", columnNames = "social_number")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"password", "socialNumber"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "social_number", nullable = false, unique = true, length = 14)
    private String socialNumber;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "phone_number", nullable = false, length = 13)
    private String phoneNumber;
    
    @Column(name = "address", nullable = false, length = 500)
    private String address;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Builder
    public User(String username, String password, String name, String socialNumber, 
                String email, String phoneNumber, String address) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.socialNumber = socialNumber;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
    
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
    
    public void updateProfile(String name, String phoneNumber, String address) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
    
    // username과 socialNumber는 불변이므로 setter 메서드를 제공하지 않음
    
    public String getMaskedSocialNumber() {
        if (socialNumber == null || socialNumber.length() < 8) {
            return "***-***";
        }
        return socialNumber.substring(0, 6) + "-*******";
    }
    
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        return phoneNumber.substring(0, 3) + "-****-" + phoneNumber.substring(9);
    }
}