package com.autoever.member.message.service;

import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대량 메시지 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkMessageService {
    
    private final AgeCalculationService ageCalculationService;
    
    // 임시로 메모리에 작업 상태 저장 (실제로는 DB나 Redis 사용)
    private final ConcurrentHashMap<UUID, BulkMessageJobStatus> jobStatusMap = new ConcurrentHashMap<>();
    
    /**
     * 대량 메시지 발송 시작
     */
    public BulkMessageResponse sendBulkMessage(MessageSendDto request) {
        UUID jobId = UUID.randomUUID();
        
        log.info("대량 메시지 발송 작업 시작 - jobId: {}, ageGroup: {}", 
                 jobId, request.ageGroup());
        
        // TODO: 실제 구현은 그룹 2, 3에서 진행
        // 지금은 기본 응답만 반환
        int estimatedUsers = getEstimatedUserCount(request.getAgeGroupEnum());
        
        BulkMessageResponse response = BulkMessageResponse.inProgress(jobId, estimatedUsers);
        
        // 작업 상태 초기화
        BulkMessageJobStatus initialStatus = new BulkMessageJobStatus(
            jobId,
            BulkMessageResponse.JobStatus.IN_PROGRESS,
            estimatedUsers,
            0, // processedUsers
            0, // successCount
            0, // failureCount
            response.startedAt(),
            null, // completedAt
            null, // duration
            0.0 // progressPercentage
        );
        
        jobStatusMap.put(jobId, initialStatus);
        
        return response;
    }
    
    /**
     * 작업 상태 조회
     */
    public BulkMessageJobStatus getJobStatus(UUID jobId) {
        BulkMessageJobStatus status = jobStatusMap.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("존재하지 않는 작업 ID입니다: " + jobId);
        }
        return status;
    }
    
    /**
     * 연령대별 예상 사용자 수 (임시)
     */
    private int getEstimatedUserCount(com.autoever.member.message.dto.AgeGroup ageGroup) {
        // 실제로는 DB에서 count 쿼리
        return switch (ageGroup) {
            case TEENS -> 5000;
            case TWENTIES -> 15000;
            case THIRTIES -> 20000;
            case FORTIES -> 18000;
            case FIFTIES_PLUS -> 12000;
        };
    }
}