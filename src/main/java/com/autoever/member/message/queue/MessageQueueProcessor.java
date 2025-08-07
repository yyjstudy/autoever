package com.autoever.member.message.queue;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.KakaoTalkApiClient;
import com.autoever.member.message.client.SmsApiClient;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.ratelimit.ApiRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 큐에서 메시지를 꺼내서 처리하는 백그라운드 프로세서
 */
@Service
public class MessageQueueProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(MessageQueueProcessor.class);
    
    private final MessageQueueService messageQueueService;
    private final ApiRateLimiter apiRateLimiter;
    private final KakaoTalkApiClient kakaoTalkApiClient;
    private final SmsApiClient smsApiClient;
    
    public MessageQueueProcessor(MessageQueueService messageQueueService, 
                               ApiRateLimiter apiRateLimiter,
                               KakaoTalkApiClient kakaoTalkApiClient,
                               SmsApiClient smsApiClient) {
        this.messageQueueService = messageQueueService;
        this.apiRateLimiter = apiRateLimiter;
        this.kakaoTalkApiClient = kakaoTalkApiClient;
        this.smsApiClient = smsApiClient;
        log.info("MessageQueueProcessor 초기화 완료");
    }
    
    /**
     * 5초마다 큐를 확인해서 처리 가능한 메시지 발송
     */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void processQueue() {
        MessageQueueItem item = messageQueueService.dequeue();
        
        if (item == null) {
            return; // 큐가 비어있음
        }
        
        log.info("큐에서 메시지 처리 시작 - {}", item);
        
        try {
            // Rate limit 확인 후 처리
            if (apiRateLimiter.tryAcquire(item.getPreferredApiType())) {
                boolean success = sendMessage(item);
                
                if (success) {
                    log.info("큐 메시지 발송 성공 - ID: {}", item.getId());
                } else {
                    log.warn("큐 메시지 발송 실패 - ID: {}", item.getId());
                    // 실패한 경우 재시도 로직을 추가할 수 있음
                }
                
            } else {
                // Rate limit 아직 초과 상태 - 큐에 다시 추가
                log.debug("Rate limit 여전히 초과 - 큐에 재추가: {}", item);
                messageQueueService.enqueue(item.getMemberName(), item.getPhoneNumber(), 
                    item.getMessage(), item.getPreferredApiType());
            }
            
        } catch (Exception e) {
            log.error("큐 메시지 처리 중 오류 발생 - ID: " + item.getId(), e);
        }
    }
    
    /**
     * 실제 메시지 발송 수행
     */
    private boolean sendMessage(MessageQueueItem item) {
        try {
            MessageRequest request = new MessageRequest(item.getPhoneNumber(), item.getMessage());
            MessageResponse response;
            
            if (item.getPreferredApiType() == ApiType.KAKAOTALK) {
                response = kakaoTalkApiClient.sendMessage(request);
                
                if (!response.success() && smsApiClient.isAvailable()) {
                    // 카카오톡 실패 시 SMS로 fallback
                    log.info("카카오톡 실패, SMS로 전환 - ID: {}", item.getId());
                    response = smsApiClient.sendMessage(request);
                }
                
            } else {
                response = smsApiClient.sendMessage(request);
            }
            
            return response.success();
            
        } catch (Exception e) {
            log.error("메시지 발송 중 오류 - ID: " + item.getId(), e);
            return false;
        }
    }
    
    /**
     * 현재 큐 상태 로깅 (1분마다)
     */
    @Scheduled(fixedDelay = 60000) // 1분마다
    public void logQueueStatus() {
        MessageQueueService.QueueStatus status = messageQueueService.getQueueStatus();
        if (status.getCurrentSize() > 0) {
            log.info("큐 상태 - 현재: {}/{} ({}%)", 
                status.getCurrentSize(), status.getMaxSize(), 
                String.format("%.1f", status.getUsagePercent()));
        }
    }
}