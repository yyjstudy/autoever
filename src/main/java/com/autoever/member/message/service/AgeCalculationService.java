package com.autoever.member.message.service;

import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.AgeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * 연령 계산 서비스
 */
@Slf4j
@Service
public class AgeCalculationService {
    
    /**
     * 주민등록번호로부터 나이 계산
     * 
     * @param socialNumber 주민등록번호 (형식: YYMMDD-GXXXXXX)
     * @return 만 나이
     */
    public int calculateAge(String socialNumber) {
        if (socialNumber == null || socialNumber.length() < 7) {
            throw new IllegalArgumentException("유효하지 않은 주민등록번호입니다");
        }
        
        // 주민등록번호에서 생년월일 추출
        String yearPrefix = socialNumber.substring(0, 2);
        String month = socialNumber.substring(2, 4);
        String day = socialNumber.substring(4, 6);
        char genderCode = socialNumber.charAt(7);
        
        // 성별 코드로 세기 판단 (1,2: 1900년대, 3,4: 2000년대)
        int year;
        if (genderCode == '1' || genderCode == '2') {
            year = 1900 + Integer.parseInt(yearPrefix);
        } else if (genderCode == '3' || genderCode == '4') {
            year = 2000 + Integer.parseInt(yearPrefix);
        } else {
            throw new IllegalArgumentException("유효하지 않은 성별 코드입니다");
        }
        
        LocalDate birthDate = LocalDate.of(year, Integer.parseInt(month), Integer.parseInt(day));
        LocalDate currentDate = LocalDate.now();
        
        // 만 나이 계산
        int age = Period.between(birthDate, currentDate).getYears();
        
        log.debug("주민등록번호 {} -> 생년월일: {}, 만 나이: {}", 
                  maskSocialNumber(socialNumber), birthDate, age);
        
        return age;
    }
    
    /**
     * 연령대로부터 연령 범위 계산
     * 
     * @param ageGroup 연령대
     * @return 연령 범위
     */
    public AgeRange calculateAgeRange(AgeGroup ageGroup) {
        return ageGroup.getAgeRange();
    }
    
    /**
     * 특정 나이가 속하는 연령대 반환
     * 
     * @param age 나이
     * @return 연령대
     */
    public AgeGroup getAgeGroup(int age) {
        for (AgeGroup group : AgeGroup.values()) {
            if (group.contains(age)) {
                return group;
            }
        }
        // 기본값으로 50대 이상 반환
        return AgeGroup.FIFTIES_PLUS;
    }
    
    /**
     * 주민등록번호 마스킹
     */
    private String maskSocialNumber(String socialNumber) {
        if (socialNumber == null || socialNumber.length() < 8) {
            return "****";
        }
        return socialNumber.substring(0, 6) + "-" + socialNumber.charAt(7) + "******";
    }
}