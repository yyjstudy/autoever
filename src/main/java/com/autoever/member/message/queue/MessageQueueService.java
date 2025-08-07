package com.autoever.member.message.queue;

import com.autoever.member.message.ApiType;
import com.autoever.member.message.result.MessageSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 메시지 큐 서비스 - Rate Limit 초과 시 대기열 관리
 */
@Service
public class MessageQueueService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageQueueService.class);
    
    // 큐 최대 크기 설정 - 1분 처리량(600)의 2.5배로 peak load 대응
    private static final int MAX_QUEUE_SIZE = 1500;
    
    private final BlockingQueue<MessageQueueItem> messageQueue;
    
    public MessageQueueService() {
        this.messageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        log.info("MessageQueueService 초기화 완료 - MAX_QUEUE_SIZE: {}", MAX_QUEUE_SIZE);
    }
    
    /**
     * Rate limit 초과 시 메시지를 큐에 추가
     * 
     * @param memberName 회원명
     * @param phoneNumber 전화번호
     * @param message 메시지
     * @param apiType API 타입
     * @return 큐 추가 성공 여부
     */
    public QueueResult enqueue(String memberName, String phoneNumber, String message, ApiType apiType) {
        MessageQueueItem item = new MessageQueueItem(memberName, phoneNumber, message, apiType);
        
        boolean added = messageQueue.offer(item); // non-blocking add
        
        if (added) {
            log.info("메시지 큐에 추가 성공 - {} (현재 큐 크기: {})", item, messageQueue.size());
            return QueueResult.queued(item.getId(), messageQueue.size());
        } else {
            log.warn("메시지 큐 용량 초과 - 큐 크기: {}, MAX: {}", messageQueue.size(), MAX_QUEUE_SIZE);
            return QueueResult.queueFull();
        }
    }
    
    /**
     * 큐에서 메시지 하나 가져오기 (처리용)
     */
    public MessageQueueItem dequeue() {
        return messageQueue.poll(); // non-blocking get
    }
    
    /**
     * 현재 큐 상태 정보
     */
    public QueueStatus getQueueStatus() {
        return new QueueStatus(messageQueue.size(), MAX_QUEUE_SIZE);
    }
    
    /**
     * 큐 결과를 나타내는 클래스
     */
    public static class QueueResult {
        private final boolean success;
        private final String queueId;
        private final int queuePosition;
        private final String message;
        
        private QueueResult(boolean success, String queueId, int queuePosition, String message) {
            this.success = success;
            this.queueId = queueId;
            this.queuePosition = queuePosition;
            this.message = message;
        }
        
        public static QueueResult queued(String queueId, int position) {
            return new QueueResult(true, queueId, position, "메시지가 대기열에 추가되었습니다.");
        }
        
        public static QueueResult queueFull() {
            return new QueueResult(false, null, -1, "대기열이 가득참니다. 나중에 다시 시도해주세요.");
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getQueueId() { return queueId; }
        public int getQueuePosition() { return queuePosition; }
        public String getMessage() { return message; }
    }
    
    /**
     * 큐 상태 정보
     */
    public static class QueueStatus {
        private final int currentSize;
        private final int maxSize;
        
        public QueueStatus(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }
        
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public boolean isFull() { return currentSize >= maxSize; }
        public double getUsagePercent() { return (double) currentSize / maxSize * 100; }
    }
}