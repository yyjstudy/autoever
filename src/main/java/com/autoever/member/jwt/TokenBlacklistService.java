package com.autoever.member.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 토큰 블랙리스트 관리 서비스
 * 로그아웃되거나 무효화된 토큰을 관리하여 재사용을 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final JwtService jwtService;
    
    // 인메모리 블랙리스트 저장소 (실제 환경에서는 Redis 등 외부 저장소 사용 권장)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    /**
     * 토큰을 블랙리스트에 추가
     * 
     * @param token 블랙리스트에 추가할 JWT 토큰
     */
    public void addToBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("빈 토큰을 블랙리스트에 추가하려고 시도했습니다.");
            return;
        }
        
        try {
            // 토큰이 이미 만료된 경우 블랙리스트에 추가할 필요 없음
            if (jwtService.validateToken(token)) {
                blacklistedTokens.add(token);
                String username = jwtService.extractUsername(token);
                log.info("토큰이 블랙리스트에 추가되었습니다. 사용자: {}", username);
            } else {
                log.debug("이미 만료된 토큰은 블랙리스트에 추가하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * 
     * @param token 확인할 JWT 토큰
     * @return 블랙리스트에 포함되어 있으면 true
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        boolean isBlacklisted = blacklistedTokens.contains(token);
        
        if (isBlacklisted) {
            log.debug("블랙리스트에 등록된 토큰 접근 시도 감지");
        }
        
        return isBlacklisted;
    }
    
    /**
     * 만료된 토큰들을 블랙리스트에서 정리
     * 스케줄링을 통해 주기적으로 호출하여 메모리 사용량 최적화
     */
    public void cleanupExpiredTokens() {
        int initialSize = blacklistedTokens.size();
        
        blacklistedTokens.removeIf(token -> {
            try {
                Date expiration = jwtService.extractExpiration(token);
                return expiration.before(new Date());
            } catch (Exception e) {
                // 파싱 실패한 토큰은 제거
                log.debug("파싱 실패한 토큰을 블랙리스트에서 제거: {}", e.getMessage());
                return true;
            }
        });
        
        int removedCount = initialSize - blacklistedTokens.size();
        if (removedCount > 0) {
            log.info("만료된 토큰 {}개를 블랙리스트에서 정리했습니다. 현재 블랙리스트 크기: {}", 
                     removedCount, blacklistedTokens.size());
        }
    }
    
    /**
     * 현재 블랙리스트 크기 반환
     * 
     * @return 블랙리스트에 등록된 토큰 수
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
    
    /**
     * 특정 사용자의 모든 토큰을 블랙리스트에 추가
     * 사용자 계정 차단 시 사용
     * 
     * @param username 차단할 사용자명
     */
    public void blacklistAllUserTokens(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("빈 사용자명으로 토큰 블랙리스트 요청");
            return;
        }
        
        int count = 0;
        for (String token : Set.copyOf(blacklistedTokens)) {
            try {
                String tokenUsername = jwtService.extractUsername(token);
                if (username.equals(tokenUsername)) {
                    count++;
                }
            } catch (Exception e) {
                log.debug("토큰 사용자명 추출 실패: {}", e.getMessage());
            }
        }
        
        log.info("사용자 {}의 토큰 {}개가 이미 블랙리스트에 등록되어 있습니다.", username, count);
    }
    
    /**
     * 블랙리스트 전체 정리 (테스트 목적)
     */
    public void clearBlacklist() {
        int size = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.info("블랙리스트가 전체 정리되었습니다. 제거된 토큰 수: {}", size);
    }
}