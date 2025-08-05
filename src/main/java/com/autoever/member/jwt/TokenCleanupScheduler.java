package com.autoever.member.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 블랙리스트 정리 스케줄러
 * 주기적으로 만료된 토큰들을 블랙리스트에서 제거하여 메모리 사용량 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jwt.token-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class TokenCleanupScheduler {
    
    private final TokenBlacklistService tokenBlacklistService;
    
    /**
     * 매시간 정각에 만료된 토큰들을 블랙리스트에서 정리
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 * * * *" = 매시간 0분 0초에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        log.debug("만료된 JWT 토큰 정리 작업 시작");
        
        try {
            int beforeSize = tokenBlacklistService.getBlacklistSize();
            tokenBlacklistService.cleanupExpiredTokens();
            int afterSize = tokenBlacklistService.getBlacklistSize();
            
            int cleanedCount = beforeSize - afterSize;
            if (cleanedCount > 0) {
                log.info("JWT 토큰 정리 완료: {}개 제거, 현재 블랙리스트 크기: {}", cleanedCount, afterSize);
            } else {
                log.debug("정리할 만료된 토큰 없음. 현재 블랙리스트 크기: {}", afterSize);
            }
        } catch (Exception e) {
            log.error("JWT 토큰 정리 작업 중 오류 발생: {}", e.getMessage(), e);
        }
        
        log.debug("만료된 JWT 토큰 정리 작업 완료");
    }
    
    /**
     * 매일 자정에 블랙리스트 상태 리포트 로깅
     * cron 표현식: "0 0 0 * * *" = 매일 자정에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyReport() {
        int blacklistSize = tokenBlacklistService.getBlacklistSize();
        log.info("일일 JWT 블랙리스트 리포트 - 현재 등록된 토큰 수: {}", blacklistSize);
        
        if (blacklistSize > 10000) {
            log.warn("JWT 블랙리스트 크기가 임계값(10,000)을 초과했습니다. 메모리 사용량을 확인해 주세요.");
        }
    }
}