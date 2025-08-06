package com.autoever.member.message.service;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.MessageApiClient;
import com.autoever.member.message.client.MessageClientFactory;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.exception.MessageApiException;
import com.autoever.member.message.retry.RetryContext;
import com.autoever.member.message.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 메시지 발송 서비스 - 재시도 로직과 모니터링을 포함한 메인 서비스
 */
@Service
public class MessageService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    
    private final MessageClientFactory clientFactory;
    private final MessageMonitoringService monitoringService;
    
    public MessageService(MessageClientFactory clientFactory, MessageMonitoringService monitoringService) {
        this.clientFactory = clientFactory;
        this.monitoringService = monitoringService;
    }
    
    /**
     * 메시지 발송 (재시도 포함)
     * 
     * @param request 메시지 요청
     * @param preferredApiType 선호하는 API 타입
     * @param retryPolicy 재시도 정책
     * @return 발송 결과
     */
    public MessageResponse sendMessage(MessageRequest request, ApiType preferredApiType, RetryPolicy retryPolicy) {
        RetryContext retryContext = new RetryContext(request, retryPolicy);
        
        log.info("메시지 발송 시작 - 수신자: {}, 선호 API: {}, 재시도 정책: {}", 
            request.recipient(), preferredApiType.getDisplayName(), retryPolicy);
        
        MessageResponse finalResponse = null;
        
        try {
            while (true) {
                // 사용 가능한 클라이언트 선택
                MessageApiClient client = clientFactory.getAvailableClient(preferredApiType);
                if (client == null) {
                    // Rate Limiting으로 인해 클라이언트를 얻지 못한 경우 잠시 대기 후 재시도
                    client = clientFactory.getAvailableClientWithTimeout(preferredApiType, 5000); // 5초 대기
                }
                
                if (client == null) {
                    throw new MessageApiException(preferredApiType, "NO_CLIENT", "사용 가능한 메시지 클라이언트가 없습니다");
                }
                
                // 시도 시작
                retryContext.startNewAttempt(client.getApiType());
                
                try {
                    // 메시지 발송 시도
                    finalResponse = client.sendMessage(request);
                    
                    if (finalResponse.success()) {
                        // 성공
                        String details = String.format("API: %s, 응답: %s", 
                            client.getApiType().getDisplayName(), finalResponse.messageId());
                        retryContext.recordSuccess(details);
                        
                        log.info("메시지 발송 성공 - 수신자: {}, API: {}, 시도: {}/{}", 
                            request.recipient(), 
                            client.getApiType().getDisplayName(),
                            retryContext.getCurrentAttempt(),
                            retryPolicy.getMaxAttempts());
                        
                        break; // 성공 시 루프 종료
                    } else {
                        // API 호출은 성공했지만 비즈니스 로직상 실패
                        MessageApiException exception = new MessageApiException(
                            client.getApiType(), "SEND_FAILED", "메시지 발송 실패: " + finalResponse.errorMessage());
                        throw exception;
                    }
                    
                } catch (MessageApiException e) {
                    // 발송 실패
                    String details = String.format("API: %s, 오류: %s", 
                        client.getApiType().getDisplayName(), e.getMessage());
                    retryContext.recordFailure(e, details);
                    
                    log.warn("메시지 발송 실패 - 수신자: {}, API: {}, 시도: {}/{}, 오류: {}", 
                        request.recipient(),
                        client.getApiType().getDisplayName(),
                        retryContext.getCurrentAttempt(),
                        retryPolicy.getMaxAttempts(),
                        e.getMessage());
                    
                    // 재시도 가능 여부 확인
                    if (!retryContext.canRetry()) {
                        log.error("메시지 발송 최종 실패 - 수신자: {}, 모든 재시도 소진", 
                            request.recipient());
                        finalResponse = MessageResponse.failure("MAX_RETRY", "모든 재시도 실패: " + e.getMessage(), preferredApiType);
                        break;
                    }
                    
                    // 재시도 지연 시간 적용
                    java.time.Duration delay = retryContext.getNextDelay();
                    if (!delay.isZero()) {
                        log.info("재시도 대기 - 수신자: {}, 대기시간: {}ms", 
                            request.recipient(), delay.toMillis());
                        
                        try {
                            Thread.sleep(delay.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("재시도 대기 중 인터럽트 발생 - 수신자: {}", 
                                request.recipient());
                            finalResponse = MessageResponse.failure("INTERRUPTED", "재시도 중 인터럽트: " + e.getMessage(), preferredApiType);
                            break;
                        }
                    }
                }
            }
            
        } finally {
            // 모니터링 정보 기록
            RetryContext.RetrySummary summary = retryContext.createSummary();
            monitoringService.recordMessageAttempt(summary);
            
            log.info("메시지 발송 완료 - {}", summary);
        }
        
        return finalResponse != null ? finalResponse : MessageResponse.failure("UNKNOWN", "알 수 없는 오류", preferredApiType);
    }
    
    /**
     * 메시지 발송 (기본 재시도 정책 사용)
     */
    public MessageResponse sendMessage(MessageRequest request, ApiType preferredApiType) {
        return sendMessage(request, preferredApiType, RetryPolicy.defaultPolicy());
    }
    
    /**
     * 메시지 발송 (KakaoTalk 우선, 기본 재시도 정책)
     */
    public MessageResponse sendMessage(MessageRequest request) {
        return sendMessage(request, ApiType.KAKAOTALK, RetryPolicy.defaultPolicy());
    }
    
    /**
     * 비동기 메시지 발송
     */
    public CompletableFuture<MessageResponse> sendMessageAsync(MessageRequest request, ApiType preferredApiType, RetryPolicy retryPolicy) {
        return CompletableFuture.supplyAsync(() -> sendMessage(request, preferredApiType, retryPolicy));
    }
    
    /**
     * 비동기 메시지 발송 (기본 설정)
     */
    public CompletableFuture<MessageResponse> sendMessageAsync(MessageRequest request) {
        return sendMessageAsync(request, ApiType.KAKAOTALK, RetryPolicy.defaultPolicy());
    }
    
    /**
     * 메시지 발송 상태 확인 (건강성 체크)
     */
    public boolean isMessageServiceHealthy() {
        try {
            MessageApiClient healthyClient = clientFactory.getHealthyClient();
            return healthyClient != null && healthyClient.isAvailable();
        } catch (Exception e) {
            log.warn("메시지 서비스 건강성 체크 실패", e);
            return false;
        }
    }
    
    /**
     * 클라이언트 팩토리 상태 새로고침
     */
    public void refreshClientStatus() {
        log.info("메시지 서비스 클라이언트 상태 새로고침");
        clientFactory.refresh();
    }
}