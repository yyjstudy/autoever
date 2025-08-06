package com.autoever.member.message.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 연령대 구분
 */
@Getter
@RequiredArgsConstructor
public enum AgeGroup {
    TEENS("10대", 10, 19),
    TWENTIES("20대", 20, 29),
    THIRTIES("30대", 30, 39),
    FORTIES("40대", 40, 49),
    FIFTIES_PLUS("50대 이상", 50, 999);
    
    private final String description;
    private final int minAge;
    private final int maxAge;
    
    /**
     * 연령 범위 반환
     */
    public AgeRange getAgeRange() {
        return new AgeRange(minAge, maxAge);
    }
    
    /**
     * 특정 나이가 해당 연령대에 속하는지 확인
     */
    public boolean contains(int age) {
        return age >= minAge && age <= maxAge;
    }
}