package com.autoever.member.message.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대량 메시지 발송 작업 상태 DTO
 */
public record BulkMessageJobStatus(
    UUID jobId,
    BulkMessageResponse.JobStatus status,
    int totalUsers,
    int processedUsers,
    int successCount,
    int failureCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    Duration duration,
    double progressPercentage
) {
    
    /**
     * 진행률 계산
     */
    public static double calculateProgress(int processedUsers, int totalUsers) {
        if (totalUsers == 0) return 100.0;
        return Math.round((double) processedUsers / totalUsers * 100 * 100) / 100.0;
    }
    
    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        if (processedUsers == 0) return 0.0;
        return Math.round((double) successCount / processedUsers * 100 * 100) / 100.0;
    }
}