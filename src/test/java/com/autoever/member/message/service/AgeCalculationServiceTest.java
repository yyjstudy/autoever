package com.autoever.member.message.service;

import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.AgeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgeCalculationService 테스트")
class AgeCalculationServiceTest {
    
    private AgeCalculationService ageCalculationService;
    
    @BeforeEach
    void setUp() {
        ageCalculationService = new AgeCalculationService();
    }
    
    @Test
    @DisplayName("1900년대생 나이 계산")
    void calculateAge_1900s() {
        // Given
        String socialNumber = "800101-1234567"; // 1980년 1월 1일생 남자
        int currentYear = LocalDate.now().getYear();
        int expectedAge = currentYear - 1980;
        
        // When
        int age = ageCalculationService.calculateAge(socialNumber);
        
        // Then
        assertThat(age).isEqualTo(expectedAge);
    }
    
    @Test
    @DisplayName("2000년대생 나이 계산")
    void calculateAge_2000s() {
        // Given
        String socialNumber = "050815-3234567"; // 2005년 8월 15일생 남자
        int currentYear = LocalDate.now().getYear();
        int expectedAge = currentYear - 2005;
        
        // When
        int age = ageCalculationService.calculateAge(socialNumber);
        
        // Then
        assertThat(age).isIn(expectedAge - 1, expectedAge); // 생일 전후 고려
    }
    
    @Test
    @DisplayName("유효하지 않은 주민등록번호 - null")
    void calculateAge_nullSocialNumber() {
        assertThatThrownBy(() -> ageCalculationService.calculateAge(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효하지 않은 주민등록번호입니다");
    }
    
    @Test
    @DisplayName("유효하지 않은 주민등록번호 - 짧은 길이")
    void calculateAge_shortSocialNumber() {
        assertThatThrownBy(() -> ageCalculationService.calculateAge("123456"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효하지 않은 주민등록번호입니다");
    }
    
    @Test
    @DisplayName("유효하지 않은 성별 코드")
    void calculateAge_invalidGenderCode() {
        assertThatThrownBy(() -> ageCalculationService.calculateAge("800101-5234567"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효하지 않은 성별 코드입니다");
    }
    
    @Test
    @DisplayName("연령대별 연령 범위 계산")
    void calculateAgeRange() {
        // When & Then
        AgeRange teensRange = ageCalculationService.calculateAgeRange(AgeGroup.TEENS);
        assertThat(teensRange.getMinAge()).isEqualTo(10);
        assertThat(teensRange.getMaxAge()).isEqualTo(19);
        
        AgeRange twentiesRange = ageCalculationService.calculateAgeRange(AgeGroup.TWENTIES);
        assertThat(twentiesRange.getMinAge()).isEqualTo(20);
        assertThat(twentiesRange.getMaxAge()).isEqualTo(29);
        
        AgeRange fiftiesPlusRange = ageCalculationService.calculateAgeRange(AgeGroup.FIFTIES_PLUS);
        assertThat(fiftiesPlusRange.getMinAge()).isEqualTo(50);
        assertThat(fiftiesPlusRange.getMaxAge()).isEqualTo(999);
    }
    
    @Test
    @DisplayName("나이로 연령대 구하기")
    void getAgeGroup() {
        assertThat(ageCalculationService.getAgeGroup(15)).isEqualTo(AgeGroup.TEENS);
        assertThat(ageCalculationService.getAgeGroup(25)).isEqualTo(AgeGroup.TWENTIES);
        assertThat(ageCalculationService.getAgeGroup(35)).isEqualTo(AgeGroup.THIRTIES);
        assertThat(ageCalculationService.getAgeGroup(45)).isEqualTo(AgeGroup.FORTIES);
        assertThat(ageCalculationService.getAgeGroup(55)).isEqualTo(AgeGroup.FIFTIES_PLUS);
        assertThat(ageCalculationService.getAgeGroup(99)).isEqualTo(AgeGroup.FIFTIES_PLUS);
    }
}