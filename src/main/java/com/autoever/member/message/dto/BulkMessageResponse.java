package com.autoever.member.message.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대량 메시지 발송 응답 DTO
 */
public record BulkMessageResponse(
    UUID jobId,
    int totalUsers,
    Duration estimatedDuration,
    JobStatus status,
    LocalDateTime startedAt
) {
    
    /**
     * 대량 메시지 발송 작업 상태
     */
    public enum JobStatus {
        CREATED("생성됨"),
        IN_PROGRESS("처리 중"),
        PROCESSING_BATCH("배치 처리 중"),
        SENDING_MESSAGES("메시지 발송 중"),
        COMPLETED("완료"),
        PARTIALLY_FAILED("일부 실패"),
        FAILED("실패"),
        CANCELLED("취소됨");
        
        private final String description;
        
        JobStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 성공 응답 생성
     */
    public static BulkMessageResponse inProgress(UUID jobId, int totalUsers) {
        // 대략적인 예상 시간 계산 (사용자 100명당 1초)
        int estimatedSeconds = Math.max(1, totalUsers / 100);
        return new BulkMessageResponse(
            jobId,
            totalUsers,
            Duration.ofSeconds(estimatedSeconds),
            JobStatus.IN_PROGRESS,
            LocalDateTime.now()
        );
    }
    
    /**
     * 큐 가득참으로 인한 실패 응답 생성
     */
    public static BulkMessageResponse queueFull(UUID jobId) {
        return new BulkMessageResponse(
            jobId,
            0,
            Duration.ZERO,
            JobStatus.FAILED,
            LocalDateTime.now()
        );
    }
}