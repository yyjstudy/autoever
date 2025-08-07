package com.autoever.member.message.result;

/**
 * 메시지 발송 결과를 나타내는 열거형
 */
public enum MessageSendResult {
    
    /**
     * 카카오톡으로 성공적으로 발송됨
     */
    SUCCESS_KAKAO("카카오톡 발송 성공"),
    
    /**
     * 카카오톡 실패 후 SMS로 대체 발송되어 성공함
     */
    SUCCESS_SMS_FALLBACK("SMS 대체 발송 성공"),
    
    /**
     * 카카오톡과 SMS 모두 실패함
     */
    FAILED_BOTH("모든 발송 방법 실패"),
    
    /**
     * Rate Limiting으로 인한 발송 지연
     */
    RATE_LIMITED("발송량 제한으로 대기 중"),
    
    /**
     * 큐에 추가되어 대기 중
     */
    QUEUED("대기열에 추가됨 - 순서대로 처리 예정"),
    
    /**
     * 큐가 가득 참 - 나중에 다시 시도 필요
     */
    QUEUE_FULL("대기열이 가득참 - 나중에 다시 시도해주세요"),
    
    /**
     * 잘못된 수신자 정보
     */
    INVALID_RECIPIENT("잘못된 수신자 정보");
    
    private final String description;
    
    MessageSendResult(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 발송이 성공했는지 확인
     * 
     * @return 성공한 경우 true (큐에 추가된 경우도 성공으로 처리)
     */
    public boolean isSuccess() {
        return this == SUCCESS_KAKAO || this == SUCCESS_SMS_FALLBACK || this == QUEUED;
    }
    
    /**
     * Fallback이 발생했는지 확인
     * 
     * @return Fallback으로 성공한 경우 true
     */
    public boolean isFallback() {
        return this == SUCCESS_SMS_FALLBACK;
    }
}