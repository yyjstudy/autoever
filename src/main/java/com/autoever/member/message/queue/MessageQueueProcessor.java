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
     * 0.1초마다 큐를 확인해서 처리 가능한 메시지 발송
     */
    @Scheduled(fixedDelay = 100) // 0.1초(100ms)마다 실행
    public void processQueue() {
        // 큐 상태 확인
        MessageQueueService.QueueStatus status = messageQueueService.getQueueStatus();
        if (status.getCurrentSize() == 0) {
            return; // 큐가 비어있음
        }
        
        // 각 API 타입별로 rate limit 확인하고 처리
        boolean kakaoAvailable = apiRateLimiter.hasCapacity(ApiType.KAKAOTALK);
        boolean smsAvailable = apiRateLimiter.hasCapacity(ApiType.SMS);
        
        if (!kakaoAvailable && !smsAvailable) {
            log.debug("모든 API Rate limit 초과 - 대기");
            return;
        }
        
        // 큐에서 항목을 확인하되, rate limit이 가능한 것만 처리
        MessageQueueItem item = messageQueueService.dequeue();
        
        if (item == null) {
            return; // 큐가 비어있음
        }
        
        try {
            boolean processed = false;
            
            // 선호하는 API 타입으로 시도
            if (item.getPreferredApiType() == ApiType.KAKAOTALK && kakaoAvailable) {
                if (apiRateLimiter.tryAcquire(ApiType.KAKAOTALK)) {
                    log.info("큐 메시지 카카오톡 발송 - ID: {}", item.getId());
                    processed = sendKakaoTalk(item);
                }
            } else if (item.getPreferredApiType() == ApiType.SMS && smsAvailable) {
                if (apiRateLimiter.tryAcquire(ApiType.SMS)) {
                    log.info("큐 메시지 SMS 발송 - ID: {}", item.getId());
                    processed = sendSms(item);
                }
            }
            
            // 선호 API가 불가능한 경우 대체 API 시도
            if (!processed && item.getPreferredApiType() == ApiType.KAKAOTALK && smsAvailable) {
                if (apiRateLimiter.tryAcquire(ApiType.SMS)) {
                    log.info("큐 메시지 카카오톡->SMS Fallback - ID: {}", item.getId());
                    processed = sendSms(item);
                }
            }
            
            // 처리되지 못한 경우 큐에서 소비하고 실패 처리
            if (!processed) {
                log.warn("Rate limit으로 처리 불가 - 메시지 소비 및 실패 처리: {}", item.getId());
            }
            
        } catch (Exception e) {
            log.error("큐 메시지 처리 중 오류 발생 - ID: " + item.getId(), e);
        }
    }
    
    /**
     * 카카오톡 메시지 발송
     */
    private boolean sendKakaoTalk(MessageQueueItem item) {
        try {
            MessageRequest request = new MessageRequest(item.getPhoneNumber(), item.getMessage());
            MessageResponse response = kakaoTalkApiClient.sendMessage(request);
            
            if (response.success()) {
                log.info("카카오톡 발송 성공 - ID: {}, MessageId: {}", item.getId(), response.messageId());
                return true;
            } else {
                log.warn("카카오톡 발송 실패 - ID: {}, Error: {}", item.getId(), response.errorMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("카카오톡 발송 중 오류 - ID: " + item.getId(), e);
            return false;
        }
    }
    
    /**
     * SMS 메시지 발송
     */
    private boolean sendSms(MessageQueueItem item) {
        try {
            MessageRequest request = new MessageRequest(item.getPhoneNumber(), item.getMessage());
            MessageResponse response = smsApiClient.sendMessage(request);
            
            if (response.success()) {
                log.info("SMS 발송 성공 - ID: {}, MessageId: {}", item.getId(), response.messageId());
                return true;
            } else {
                log.warn("SMS 발송 실패 - ID: {}, Error: {}", item.getId(), response.errorMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("SMS 발송 중 오류 - ID: " + item.getId(), e);
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