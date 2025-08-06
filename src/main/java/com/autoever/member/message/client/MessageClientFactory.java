package com.autoever.member.message.client;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 메시지 API 클라이언트 팩토리
 * Rate Limiting과 함께 적절한 클라이언트를 선택하고 관리합니다.
 */
@Component
public class MessageClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(MessageClientFactory.class);
    
    private final List<MessageApiClient> clients;
    private final RateLimiter rateLimiter;
    
    public MessageClientFactory(List<MessageApiClient> clients, RateLimiter rateLimiter) {
        this.clients = clients;
        this.rateLimiter = rateLimiter;
        
        log.info("MessageClientFactory 초기화 완료 - 등록된 클라이언트 수: {}", clients.size());
        for (MessageApiClient client : clients) {
            log.info("등록된 클라이언트: {}", 
                client.getApiType().getDisplayName());
        }
    }
    
    /**
     * 선호하는 API 타입의 사용 가능한 클라이언트 반환
     * Rate Limiting 확인 후 토큰이 있는 클라이언트를 반환합니다.
     * 
     * @param preferredType 선호하는 API 타입
     * @return 사용 가능한 클라이언트 (없으면 null)
     */
    public MessageApiClient getAvailableClient(ApiType preferredType) {
        // 1. 선호하는 타입의 클라이언트 먼저 확인
        MessageApiClient preferredClient = getClientByType(preferredType);
        if (preferredClient != null && isClientUsable(preferredClient)) {
            return preferredClient;
        }
        
        // 2. 다른 사용 가능한 클라이언트 찾기
        for (MessageApiClient client : clients) {
            if (client.getApiType() != preferredType && isClientUsable(client)) {
                log.info("선호 타입({}) 사용 불가, 대체 클라이언트 사용: {}", 
                    preferredType.getDisplayName(), client.getApiType().getDisplayName());
                return client;
            }
        }
        
        log.warn("사용 가능한 메시지 클라이언트가 없습니다");
        return null;
    }
    
    /**
     * Rate Limiting을 고려한 건강한 클라이언트 반환
     * 토큰을 일정 시간 대기해서라도 획득하려고 시도합니다.
     * 
     * @param preferredType 선호하는 API 타입
     * @param timeoutMs 대기 시간 (밀리초)
     * @return 사용 가능한 클라이언트 (없으면 null)
     */
    public MessageApiClient getAvailableClientWithTimeout(ApiType preferredType, long timeoutMs) {
        // 1. 선호하는 타입의 클라이언트 먼저 시도 (대기 포함)
        MessageApiClient preferredClient = getClientByType(preferredType);
        if (preferredClient != null && preferredClient.isAvailable()) {
            if (rateLimiter.tryAcquire(preferredType, timeoutMs, TimeUnit.MILLISECONDS)) {
                log.debug("Rate Limiting 대기 후 토큰 획득 성공: {}", preferredType.getDisplayName());
                return preferredClient;
            }
        }
        
        // 2. 다른 클라이언트들 즉시 확인 (대기 없음)
        for (MessageApiClient client : clients) {
            if (client.getApiType() != preferredType && isClientUsable(client, false)) {
                log.info("선호 타입({}) 토큰 부족, 대체 클라이언트 즉시 사용: {}", 
                    preferredType.getDisplayName(), client.getApiType().getDisplayName());
                return client;
            }
        }
        
        log.warn("타임아웃({}ms) 내에 사용 가능한 클라이언트를 찾을 수 없습니다", timeoutMs);
        return null;
    }
    
    /**
     * 현재 가장 건강한 클라이언트 반환 (Rate Limiting 무시)
     * 긴급한 경우나 모니터링 용도로 사용
     */
    public MessageApiClient getHealthyClient() {
        for (MessageApiClient client : clients) {
            if (client.isAvailable()) {
                log.debug("건강한 클라이언트 반환: {}", client.getApiType().getDisplayName());
                return client;
            }
        }
        
        log.warn("건강한 클라이언트가 없습니다");
        return null;
    }
    
    /**
     * 모든 클라이언트 반환
     */
    public List<MessageApiClient> getAllClients() {
        return List.copyOf(clients);
    }
    
    /**
     * 특정 API 타입의 클라이언트 사용 가능 여부 확인
     */
    public boolean isClientAvailable(ApiType apiType) {
        MessageApiClient client = getClientByType(apiType);
        return client != null && isClientUsable(client);
    }
    
    /**
     * 클라이언트 상태 새로고침
     * 연결 상태를 다시 확인합니다.
     */
    public void refresh() {
        log.info("MessageClientFactory 상태 새로고침 시작");
        
        for (MessageApiClient client : clients) {
            boolean isHealthy = client.validateConnection();
            log.info("클라이언트 상태: {} - 건강: {}, 사용가능한 토큰: {}", 
                client.getApiType().getDisplayName(), 
                isHealthy,
                rateLimiter.getAvailableTokens(client.getApiType())
            );
        }
    }
    
    /**
     * API 타입으로 클라이언트 찾기
     */
    private MessageApiClient getClientByType(ApiType apiType) {
        return clients.stream()
            .filter(client -> client.getApiType() == apiType)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 클라이언트가 사용 가능한지 확인 (기본적으로 토큰 확인 포함)
     */
    private boolean isClientUsable(MessageApiClient client) {
        return isClientUsable(client, true);
    }
    
    /**
     * 클라이언트가 사용 가능한지 확인
     * 
     * @param client 클라이언트
     * @param checkRateLimit Rate Limiting 토큰 확인 여부
     */
    private boolean isClientUsable(MessageApiClient client, boolean checkRateLimit) {
        if (!client.isAvailable()) {
            return false;
        }
        
        if (checkRateLimit && !rateLimiter.tryAcquire(client.getApiType())) {
            return false;
        }
        
        return true;
    }
}