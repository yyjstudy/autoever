package com.autoever.member.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 연령 범위
 */
@Getter
@AllArgsConstructor
public class AgeRange {
    private final int minAge;
    private final int maxAge;
    
    /**
     * 특정 나이가 범위에 포함되는지 확인
     */
    public boolean contains(int age) {
        return age >= minAge && age <= maxAge;
    }
    
    @Override
    public String toString() {
        return String.format("%d-%d", minAge, maxAge);
    }
}