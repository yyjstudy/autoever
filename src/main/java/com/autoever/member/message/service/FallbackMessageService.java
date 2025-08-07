package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.KakaoTalkApiClient;
import com.autoever.member.message.client.MessageApiClient;
import com.autoever.member.message.client.SmsApiClient;
import com.autoever.member.message.ratelimit.ApiRateLimiter;
import com.autoever.member.message.queue.MessageQueueService;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
import com.autoever.member.message.logging.MessageTraceContext;
import com.autoever.member.message.result.MessageSendResult;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.template.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오톡 발송 실패 시 SMS로 자동 전환하는 Fallback 메커니즘을 구현하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackMessageService {

    private final KakaoTalkApiClient kakaoTalkApiClient;
    private final SmsApiClient smsApiClient;
    private final MessageTemplateService messageTemplateService;
    private final MessageSendTracker messageSendTracker;
    private final ApiRateLimiter apiRateLimiter;
    private final MessageQueueService messageQueueService;

    /**
     * 템플릿이 적용된 메시지를 Fallback 메커니즘과 함께 발송합니다.
     * 
     * @param user 수신자 정보
     * @param originalMessage 원본 메시지 (템플릿 적용 전)
     * @return 최종 발송 결과
     */
    public MessageSendResult sendWithFallback(User user, String originalMessage) {
        if (user == null) {
            log.error("사용자 정보가 null입니다");
            throw new IllegalArgumentException("사용자 정보는 필수입니다");
        }

        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            log.error("메시지 내용이 비어있습니다");
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }

        String maskedPhone = maskPhoneNumber(user.getPhoneNumber());
        String traceId = MessageTraceContext.startTrace(maskedPhone, "template");
        
        try {
            log.info("메시지 발송 요청 - 수신자: {}, 메시지 길이: {}", maskedPhone, originalMessage.length());

            // 1. 템플릿 적용
            long templateStartTime = System.currentTimeMillis();
            String templatedMessage = messageTemplateService.applyTemplate(user, originalMessage);
            long templateDuration = System.currentTimeMillis() - templateStartTime;
            
            log.info("템플릿 적용 완료 - 처리시간: {}ms, 템플릿 후 길이: {}", 
                templateDuration, templatedMessage.length());

            // 2. 큐에 추가 시도 (카카오톡 우선)
            MessageQueueService.QueueResult queueResult = messageQueueService.enqueue(
                user.getName(), user.getPhoneNumber(), templatedMessage, ApiType.KAKAOTALK);
            
            if (queueResult.isSuccess()) {
                log.info("메시지 큐에 추가 완료 - QueueId: {}, Position: {}", 
                    queueResult.getQueueId(), queueResult.getQueuePosition());
                messageSendTracker.recordResult(MessageSendResult.QUEUED, ApiType.KAKAOTALK);
                return MessageSendResult.QUEUED;
            } else {
                log.error("큐 용량 초과 - {}", queueResult.getMessage());
                messageSendTracker.recordResult(MessageSendResult.QUEUE_FULL, ApiType.KAKAOTALK);
                return MessageSendResult.QUEUE_FULL;
            }
            
        } finally {
            MessageTraceContext.endTrace();
        }
    }

    /**
     * 회원 이름과 메시지로 Fallback 발송을 수행합니다.
     * 
     * @param memberName 회원 이름
     * @param phoneNumber 전화번호
     * @param originalMessage 원본 메시지
     * @return 최종 발송 결과
     */
    public MessageSendResult sendWithFallback(String memberName, String phoneNumber, String originalMessage) {
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new IllegalArgumentException("회원 이름은 필수입니다");
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("전화번호는 필수입니다");
        }

        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }

        String maskedPhone = maskPhoneNumber(phoneNumber);
        String traceId = MessageTraceContext.startTrace(maskedPhone, "direct");
        
        try {
            log.info("메시지 발송 요청 - 수신자: {}, 회원명: {}, 메시지 길이: {}", 
                maskedPhone, memberName, originalMessage.length());

            // 1. 템플릿 적용
            long templateStartTime = System.currentTimeMillis();
            String templatedMessage = messageTemplateService.applyTemplate(memberName, originalMessage);
            long templateDuration = System.currentTimeMillis() - templateStartTime;
            
            log.info("템플릿 적용 완료 - 처리시간: {}ms, 템플릿 후 길이: {}", 
                templateDuration, templatedMessage.length());

            // 2. 큐에 추가 시도 (카카오톡 우선)
            MessageQueueService.QueueResult queueResult = messageQueueService.enqueue(
                memberName, phoneNumber, templatedMessage, ApiType.KAKAOTALK);
            
            if (queueResult.isSuccess()) {
                log.info("메시지 큐에 추가 완료 - QueueId: {}, Position: {}", 
                    queueResult.getQueueId(), queueResult.getQueuePosition());
                messageSendTracker.recordResult(MessageSendResult.QUEUED, ApiType.KAKAOTALK);
                return MessageSendResult.QUEUED;
            } else {
                log.error("큐 용량 초과 - {}", queueResult.getMessage());
                messageSendTracker.recordResult(MessageSendResult.QUEUE_FULL, ApiType.KAKAOTALK);
                return MessageSendResult.QUEUE_FULL;
            }
            
        } finally {
            MessageTraceContext.endTrace();
        }
    }

    /**
     * KakaoTalk 발송을 시도합니다.
     * 
     * @param phoneNumber 전화번호
     * @param message 발송할 메시지
     * @return KakaoTalk 발송 결과
     */
    private MessageSendResult attemptKakaoTalkSend(String phoneNumber, String message) {
        MessageTraceContext.setApiType("KAKAOTALK");
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("KakaoTalk API 호출 시작 - 메시지 길이: {}", message.length());
            
            // 1. Rate Limiting 사전 검사 - Mock 서버에 불필요한 요청 방지
            if (!apiRateLimiter.tryAcquire(ApiType.KAKAOTALK)) {
                ApiRateLimiter.RateLimitInfo rateLimitInfo = apiRateLimiter.getCurrentUsage(ApiType.KAKAOTALK);
                log.warn("KakaoTalk Rate Limit 초과 - Mock 서버 호출 생략: {}", rateLimitInfo);
                return MessageSendResult.RATE_LIMITED;
            }
            
            // 2. 클라이언트 연결 상태 확인
            if (!kakaoTalkApiClient.isAvailable()) {
                log.warn("KakaoTalk 클라이언트 사용 불가 - 연결 상태 불량");
                return MessageSendResult.FAILED_BOTH;
            }

            // 3. 실제 API 호출 (Rate Limiting 통과 후)
            MessageRequest request = new MessageRequest(phoneNumber, message);
            MessageResponse response = kakaoTalkApiClient.sendMessage(request);
            long duration = System.currentTimeMillis() - startTime;

            if (response.success()) {
                log.debug("KakaoTalk API 호출 성공 - 처리시간: {}ms, messageId: {}", 
                    duration, response.messageId());
                return MessageSendResult.SUCCESS_KAKAO;
            } else {
                log.warn("KakaoTalk API 호출 실패 - 처리시간: {}ms, errorCode: {}, errorMessage: {}", 
                    duration, response.errorCode(), response.errorMessage());
                
                // Rate Limiting 상태 확인
                if ("RATE_LIMIT_EXCEEDED".equals(response.errorCode())) {
                    log.info("KakaoTalk Rate Limit 초과 - 다음 API로 전환");
                    return MessageSendResult.RATE_LIMITED;
                }
                
                return MessageSendResult.FAILED_BOTH;
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("KakaoTalk API 호출 중 예외 발생 - 처리시간: {}ms, 수신자: {}", 
                duration, maskPhoneNumber(phoneNumber), e);
            return MessageSendResult.FAILED_BOTH;
        }
    }

    /**
     * SMS 발송을 시도합니다.
     * 
     * @param phoneNumber 전화번호
     * @param message 발송할 메시지
     * @return SMS 발송 결과
     */
    private MessageSendResult attemptSmsSend(String phoneNumber, String message) {
        MessageTraceContext.setApiType("SMS");
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("SMS API 호출 시작 - 메시지 길이: {}", message.length());
            
            // 1. Rate Limiting 사전 검사 - Mock 서버에 불필요한 요청 방지
            if (!apiRateLimiter.tryAcquire(ApiType.SMS)) {
                ApiRateLimiter.RateLimitInfo rateLimitInfo = apiRateLimiter.getCurrentUsage(ApiType.SMS);
                log.warn("SMS Rate Limit 초과 - Mock 서버 호출 생략: {}", rateLimitInfo);
                return MessageSendResult.RATE_LIMITED;
            }
            
            // 2. 클라이언트 연결 상태 확인
            if (!smsApiClient.isAvailable()) {
                log.warn("SMS 클라이언트 사용 불가 - 연결 상태 불량");
                return MessageSendResult.FAILED_BOTH;
            }

            // 3. 실제 API 호출 (Rate Limiting 통과 후)
            MessageRequest request = new MessageRequest(phoneNumber, message);
            MessageResponse response = smsApiClient.sendMessage(request);
            long duration = System.currentTimeMillis() - startTime;

            if (response.success()) {
                log.debug("SMS API 호출 성공 - 처리시간: {}ms, messageId: {}", 
                    duration, response.messageId());
                return MessageSendResult.SUCCESS_SMS_FALLBACK;
            } else {
                log.warn("SMS API 호출 실패 - 처리시간: {}ms, errorCode: {}, errorMessage: {}", 
                    duration, response.errorCode(), response.errorMessage());
                
                // Rate Limiting 상태 확인
                if ("RATE_LIMIT_EXCEEDED".equals(response.errorCode())) {
                    log.info("SMS Rate Limit 초과 - 모든 채널 소진");
                    return MessageSendResult.RATE_LIMITED;
                }
                
                return MessageSendResult.FAILED_BOTH;
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("SMS API 호출 중 예외 발생 - 처리시간: {}ms, 수신자: {}", 
                duration, maskPhoneNumber(phoneNumber), e);
            return MessageSendResult.FAILED_BOTH;
        }
    }

    /**
     * 전화번호를 마스킹합니다.
     * 
     * @param phoneNumber 원본 전화번호
     * @return 마스킹된 전화번호
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 9) {
            return "***-****-****";
        }
        
        if (phoneNumber.contains("-") && phoneNumber.length() == 13) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        return "***-****-****";
    }
}