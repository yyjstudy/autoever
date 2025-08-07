package com.autoever.member.message.queue;

import com.autoever.member.message.ApiType;

import java.time.Instant;
import java.util.UUID;

/**
 * 큐에 저장될 메시지 요청 정보
 */
public class MessageQueueItem {
    private final String id;
    private final String memberName;
    private final String phoneNumber;
    private final String message;
    private final ApiType preferredApiType;
    private final Instant queuedAt;

    public MessageQueueItem(String memberName, String phoneNumber, String message, ApiType preferredApiType) {
        this.id = UUID.randomUUID().toString();
        this.memberName = memberName;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.preferredApiType = preferredApiType;
        this.queuedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getMemberName() { return memberName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public ApiType getPreferredApiType() { return preferredApiType; }
    public Instant getQueuedAt() { return queuedAt; }

    @Override
    public String toString() {
        return String.format("MessageQueueItem{id='%s', phone='%s', apiType=%s, queuedAt=%s}", 
            id, phoneNumber, preferredApiType, queuedAt);
    }
}