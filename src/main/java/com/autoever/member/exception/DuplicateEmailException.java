package com.autoever.member.exception;

import org.springframework.http.HttpStatus;

/**
 * 이메일 중복 예외 클래스
 * 회원가입 시 이미 존재하는 이메일로 가입하려 할 때 발생
 */
public class DuplicateEmailException extends BaseCustomException {
    
    /**
     * 기본 생성자
     * 
     * @param email 중복된 이메일 주소
     */
    public DuplicateEmailException(String email) {
        super("이미 존재하는 이메일입니다: " + maskEmail(email), HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
    }
    
    /**
     * DuplicateEmailException 생성을 위한 정적 팩토리 메서드
     * 
     * @param email 중복된 이메일 주소
     * @return DuplicateEmailException 인스턴스
     */
    public static DuplicateEmailException of(String email) {
        return new DuplicateEmailException(email);
    }
    
    /**
     * 이메일 마스킹 처리
     * 보안을 위해 이메일 주소의 일부를 마스킹 처리
     * 예: user@example.com -> u***@example.com
     * 
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() <= 1) {
            return "*@" + domainPart;
        } else if (localPart.length() <= 3) {
            return localPart.charAt(0) + "**@" + domainPart;
        } else {
            return localPart.charAt(0) + "***@" + domainPart;
        }
    }
}