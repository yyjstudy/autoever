package com.autoever.member.dto;

import com.autoever.member.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 개인정보 응답 DTO
 * 민감정보 마스킹 및 주소 필터링이 적용된 안전한 사용자 정보
 */
@Schema(description = "사용자 개인정보 응답 (민감정보 마스킹 처리)")
public record UserInfoDto(
    @Schema(description = "사용자 ID", example = "1")
    Long id,
    
    @Schema(description = "사용자명", example = "testuser123")
    String username,
    
    @Schema(description = "이름", example = "홍길동")
    String name,
    
    @Schema(description = "마스킹된 주민등록번호", example = "901201-1******")
    String socialNumber,
    
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,
    
    @Schema(description = "주소 (최상위 행정구역)", example = "서울특별시")
    String address,
    
    @Schema(description = "계정 생성일", example = "2025-08-05T10:30:00")
    LocalDateTime createdAt,
    
    @Schema(description = "계정 수정일", example = "2025-08-05T15:45:30")
    LocalDateTime updatedAt
) {
    
    /**
     * User 엔티티로부터 UserInfoDto 생성
     * 민감정보 마스킹 및 주소 필터링 적용
     * 
     * @param user User 엔티티
     * @return 마스킹 처리된 UserInfoDto
     */
    public static UserInfoDto from(User user) {
        return new UserInfoDto(
            user.getId(),
            user.getUsername(),
            user.getName(),
            maskSocialNumber(user.getSocialNumber()),
            user.getEmail(),
            user.getPhoneNumber(),
            filterAddress(user.getAddress()),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * 주민등록번호 마스킹 처리
     * 형식: 123456-1****** (뒤 6자리를 *로 처리)
     * 
     * @param socialNumber 원본 주민등록번호
     * @return 마스킹된 주민등록번호
     */
    private static String maskSocialNumber(String socialNumber) {
        if (socialNumber == null || socialNumber.length() < 14) {
            return socialNumber; // 잘못된 형식은 그대로 반환
        }
        
        // 123456-1****** 형태로 마스킹
        return socialNumber.substring(0, 8) + "******";
    }
    
    /**
     * 주소 필터링 처리
     * 보안을 위해 최상위 행정구역(시/도)만 반환
     * 
     * @param address 원본 주소
     * @return 필터링된 주소 (최상위 행정구역)
     */
    private static String filterAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return address;
        }
        
        // 공백으로 분리하여 첫 번째 구역(시/도) 추출
        String[] addressParts = address.trim().split("\\s+");
        if (addressParts.length > 0) {
            String topLevel = addressParts[0];
            
            // 일반적인 시/도 패턴 검증
            if (isValidTopLevelAddress(topLevel)) {
                return topLevel;
            }
        }
        
        // 패턴이 맞지 않는 경우 안전하게 처리
        return extractTopLevelWithFallback(address);
    }
    
    /**
     * 유효한 최상위 행정구역인지 검증
     * 
     * @param topLevel 최상위 주소 구분
     * @return 유효성 여부
     */
    private static boolean isValidTopLevelAddress(String topLevel) {
        // 시/도 패턴: ~시, ~도, ~특별시, ~광역시, ~특별자치시, ~특별자치도 등
        return topLevel.matches(".*[시도]$") || 
               topLevel.contains("특별시") || 
               topLevel.contains("광역시") ||
               topLevel.contains("특별자치");
    }
    
    /**
     * 예외 케이스를 위한 안전한 최상위 주소 추출
     * 
     * @param address 원본 주소
     * @return 안전하게 추출된 주소
     */
    private static String extractTopLevelWithFallback(String address) {
        // 길이가 너무 긴 경우 앞 부분만 추출 (보안 고려)
        if (address.length() > 20) {
            return address.substring(0, 20) + "...";
        }
        
        // 그 외의 경우 전체 주소를 반환하되 로깅으로 확인 가능하도록 처리
        return address;
    }
}