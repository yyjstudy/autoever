package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 전화번호 중복 예외 클래스
 * 회원가입 시 이미 존재하는 전화번호로 가입하려 할 때 발생
 */
public class DuplicatePhoneNumberException extends BaseCustomException {
    
    /**
     * 기본 생성자
     * 
     * @param phoneNumber 중복된 전화번호
     */
    public DuplicatePhoneNumberException(String phoneNumber) {
        super("이미 존재하는 전화번호입니다: " + maskPhoneNumber(phoneNumber), HttpStatus.CONFLICT, "DUPLICATE_PHONE_NUMBER");
    }
    
    /**
     * DuplicatePhoneNumberException 생성을 위한 정적 팩토리 메서드
     * 
     * @param phoneNumber 중복된 전화번호
     * @return DuplicatePhoneNumberException 인스턴스
     */
    public static DuplicatePhoneNumberException of(String phoneNumber) {
        return new DuplicatePhoneNumberException(phoneNumber);
    }
    
    /**
     * 전화번호 마스킹 처리
     * 보안을 위해 전화번호의 일부를 마스킹 처리
     * 예: 010-1234-5678 -> 010-****-5678
     * 
     * @param phoneNumber 원본 전화번호
     * @return 마스킹된 전화번호
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        
        // 010-1234-5678 형식인 경우
        if (phoneNumber.contains("-") && phoneNumber.length() == 13) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        // 01012345678 형식인 경우
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        
        // 기타 형식
        return "***-****-****";
    }
}