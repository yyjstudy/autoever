package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.ApiType;
import com.autoever.member.message.client.MessageApiClient;
import com.autoever.member.message.client.MessageClientFactory;
import com.autoever.member.message.dto.MessageRequest;
import com.autoever.member.message.dto.MessageResponse;
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

    private final MessageClientFactory messageClientFactory;
    private final MessageTemplateService messageTemplateService;
    private final MessageSendTracker messageSendTracker;

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

        // 1. 템플릿 적용
        String templatedMessage = messageTemplateService.applyTemplate(user, originalMessage);
        log.info("템플릿 적용 완료: recipient={}", maskPhoneNumber(user.getPhoneNumber()));

        // 2. KakaoTalk 우선 발송 시도
        MessageSendResult kakaoResult = attemptKakaoTalkSend(user.getPhoneNumber(), templatedMessage);
        
        if (kakaoResult == MessageSendResult.SUCCESS_KAKAO) {
            log.info("카카오톡 발송 성공: recipient={}", maskPhoneNumber(user.getPhoneNumber()));
            messageSendTracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
            return MessageSendResult.SUCCESS_KAKAO;
        }

        // 3. KakaoTalk 실패 시 SMS Fallback 시도
        log.warn("카카오톡 발송 실패, SMS로 전환: recipient={}, kakaoResult={}", 
            maskPhoneNumber(user.getPhoneNumber()), kakaoResult);

        MessageSendResult smsResult = attemptSmsSend(user.getPhoneNumber(), templatedMessage);
        
        if (smsResult == MessageSendResult.SUCCESS_SMS_FALLBACK) {
            log.info("SMS Fallback 발송 성공: recipient={}", maskPhoneNumber(user.getPhoneNumber()));
            messageSendTracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
            return MessageSendResult.SUCCESS_SMS_FALLBACK;
        }

        // 4. Rate Limiting 상태 처리
        if (smsResult == MessageSendResult.RATE_LIMITED) {
            log.warn("SMS도 Rate Limit 초과: recipient={}", maskPhoneNumber(user.getPhoneNumber()));
            messageSendTracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.SMS);
            return MessageSendResult.RATE_LIMITED;
        }
        
        if (kakaoResult == MessageSendResult.RATE_LIMITED) {
            log.warn("카카오톡 Rate Limit 초과, SMS 실패: recipient={}", maskPhoneNumber(user.getPhoneNumber()));
            messageSendTracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.KAKAOTALK);
            return MessageSendResult.RATE_LIMITED;
        }

        // 5. 모든 발송 방법 실패
        log.error("모든 발송 방법 실패: recipient={}, kakaoResult={}, smsResult={}", 
            maskPhoneNumber(user.getPhoneNumber()), kakaoResult, smsResult);
        
        messageSendTracker.recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
        return MessageSendResult.FAILED_BOTH;
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

        // 1. 템플릿 적용
        String templatedMessage = messageTemplateService.applyTemplate(memberName, originalMessage);
        log.info("템플릿 적용 완료: recipient={}", maskPhoneNumber(phoneNumber));

        // 2. KakaoTalk 우선 발송 시도
        MessageSendResult kakaoResult = attemptKakaoTalkSend(phoneNumber, templatedMessage);
        
        if (kakaoResult == MessageSendResult.SUCCESS_KAKAO) {
            log.info("카카오톡 발송 성공: recipient={}", maskPhoneNumber(phoneNumber));
            messageSendTracker.recordResult(MessageSendResult.SUCCESS_KAKAO, ApiType.KAKAOTALK);
            return MessageSendResult.SUCCESS_KAKAO;
        }

        // 3. KakaoTalk 실패 시 SMS Fallback 시도
        log.warn("카카오톡 발송 실패, SMS로 전환: recipient={}, kakaoResult={}", 
            maskPhoneNumber(phoneNumber), kakaoResult);

        MessageSendResult smsResult = attemptSmsSend(phoneNumber, templatedMessage);
        
        if (smsResult == MessageSendResult.SUCCESS_SMS_FALLBACK) {
            log.info("SMS Fallback 발송 성공: recipient={}", maskPhoneNumber(phoneNumber));
            messageSendTracker.recordResult(MessageSendResult.SUCCESS_SMS_FALLBACK, ApiType.SMS);
            return MessageSendResult.SUCCESS_SMS_FALLBACK;
        }

        // 4. Rate Limiting 상태 처리
        if (smsResult == MessageSendResult.RATE_LIMITED) {
            log.warn("SMS도 Rate Limit 초과: recipient={}", maskPhoneNumber(phoneNumber));
            messageSendTracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.SMS);
            return MessageSendResult.RATE_LIMITED;
        }
        
        if (kakaoResult == MessageSendResult.RATE_LIMITED) {
            log.warn("카카오톡 Rate Limit 초과, SMS 실패: recipient={}", maskPhoneNumber(phoneNumber));
            messageSendTracker.recordResult(MessageSendResult.RATE_LIMITED, ApiType.KAKAOTALK);
            return MessageSendResult.RATE_LIMITED;
        }

        // 5. 모든 발송 방법 실패
        log.error("모든 발송 방법 실패: recipient={}, kakaoResult={}, smsResult={}", 
            maskPhoneNumber(phoneNumber), kakaoResult, smsResult);
        
        messageSendTracker.recordResult(MessageSendResult.FAILED_BOTH, ApiType.KAKAOTALK);
        return MessageSendResult.FAILED_BOTH;
    }

    /**
     * KakaoTalk 발송을 시도합니다.
     * 
     * @param phoneNumber 전화번호
     * @param message 발송할 메시지
     * @return KakaoTalk 발송 결과
     */
    private MessageSendResult attemptKakaoTalkSend(String phoneNumber, String message) {
        try {
            MessageApiClient kakaoClient = messageClientFactory.getAvailableClient(ApiType.KAKAOTALK);
            if (kakaoClient == null) {
                log.warn("KakaoTalk 클라이언트를 찾을 수 없습니다");
                return MessageSendResult.FAILED_BOTH;
            }

            MessageRequest request = new MessageRequest(phoneNumber, message);
            MessageResponse response = kakaoClient.sendMessage(request);

            if (response.success()) {
                return MessageSendResult.SUCCESS_KAKAO;
            } else {
                log.warn("KakaoTalk 발송 실패: errorCode={}, errorMessage={}", 
                    response.errorCode(), response.errorMessage());
                
                // Rate Limiting 상태 확인
                if ("RATE_LIMIT_EXCEEDED".equals(response.errorCode())) {
                    log.info("KakaoTalk Rate Limit 초과, 상태 반환");
                    return MessageSendResult.RATE_LIMITED;
                }
                
                return MessageSendResult.FAILED_BOTH;
            }

        } catch (Exception e) {
            log.error("KakaoTalk 발송 중 예외 발생: recipient={}", maskPhoneNumber(phoneNumber), e);
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
        try {
            MessageApiClient smsClient = messageClientFactory.getAvailableClient(ApiType.SMS);
            if (smsClient == null) {
                log.warn("SMS 클라이언트를 찾을 수 없습니다");
                return MessageSendResult.FAILED_BOTH;
            }

            MessageRequest request = new MessageRequest(phoneNumber, message);
            MessageResponse response = smsClient.sendMessage(request);

            if (response.success()) {
                return MessageSendResult.SUCCESS_SMS_FALLBACK;
            } else {
                log.warn("SMS 발송 실패: errorCode={}, errorMessage={}", 
                    response.errorCode(), response.errorMessage());
                
                // Rate Limiting 상태 확인
                if ("RATE_LIMIT_EXCEEDED".equals(response.errorCode())) {
                    log.info("SMS Rate Limit 초과, 상태 반환");
                    return MessageSendResult.RATE_LIMITED;
                }
                
                return MessageSendResult.FAILED_BOTH;
            }

        } catch (Exception e) {
            log.error("SMS 발송 중 예외 발생: recipient={}", maskPhoneNumber(phoneNumber), e);
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