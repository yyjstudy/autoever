package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.message.service.BulkMessageService;
import com.autoever.member.message.service.MessagePerformanceService;
import com.autoever.member.message.service.DynamicBatchOptimizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자 메시지 발송 API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Message API", description = "관리자 메시지 발송 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminMessageController {
    
    private final BulkMessageService bulkMessageService;
    private final MessagePerformanceService performanceService;
    private final DynamicBatchOptimizer batchOptimizer;
    
    /**
     * 연령대별 대량 메시지 발송
     */
    @PostMapping("/send")
    @Operation(summary = "연령대별 대량 메시지 발송", description = "특정 연령대의 모든 회원에게 메시지를 발송합니다")
    public ResponseEntity<ApiResponse<BulkMessageResponse>> sendBulkMessage(
            @Valid @RequestBody MessageSendDto request) {
        
        BulkMessageResponse response = bulkMessageService.sendBulkMessage(request);
        
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)  // 202 - 비동기 작업 시작됨
            .body(ApiResponse.success("대량 메시지 발송이 시작되었습니다.", response));
    }
    
    /**
     * 메시지 발송 작업 상태 조회
     */
    @GetMapping("/send/{jobId}/status")
    @Operation(summary = "메시지 발송 작업 상태 조회", description = "진행 중인 메시지 발송 작업의 상태를 조회합니다")
    public ResponseEntity<ApiResponse<BulkMessageJobStatus>> getJobStatus(
            @PathVariable UUID jobId) {
        
        BulkMessageJobStatus status = bulkMessageService.getJobStatus(jobId);
        
        return ResponseEntity
            .ok(ApiResponse.success("작업 상태 조회 성공", status));
    }
    
    /**
     * 시스템 성능 메트릭 조회
     */
    @GetMapping("/performance/metrics")
    @Operation(summary = "시스템 성능 메트릭 조회", description = "전체 시스템의 성능 메트릭을 조회합니다")
    public ResponseEntity<ApiResponse<MessagePerformanceService.SystemMetrics>> getSystemMetrics() {
        
        MessagePerformanceService.SystemMetrics metrics = performanceService.getSystemMetrics();
        
        return ResponseEntity
            .ok(ApiResponse.success("시스템 메트릭 조회 성공", metrics));
    }
    
    /**
     * 배치 크기 최적화 정보 조회
     */
    @GetMapping("/performance/batch-optimization")
    @Operation(summary = "배치 크기 최적화 정보 조회", description = "현재 시스템 상태에 따른 최적 배치 크기를 조회합니다")
    public ResponseEntity<ApiResponse<DynamicBatchOptimizer.BatchSizeRecommendation>> getBatchOptimization() {
        
        DynamicBatchOptimizer.BatchSizeRecommendation recommendation = batchOptimizer.getRecommendation();
        
        return ResponseEntity
            .ok(ApiResponse.success("배치 최적화 정보 조회 성공", recommendation));
    }
    
    /**
     * 배치 크기 수동 설정
     */
    @PostMapping("/performance/batch-size")
    @Operation(summary = "배치 크기 수동 설정", description = "배치 크기를 수동으로 설정합니다")
    public ResponseEntity<ApiResponse<String>> setBatchSize(@RequestParam int batchSize) {
        
        if (batchSize < 50 || batchSize > 2000) {
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("배치 크기는 50-2000 사이여야 합니다"));
        }
        
        int oldSize = batchOptimizer.getCurrentBatchSize();
        batchOptimizer.setBatchSize(batchSize);
        
        String message = String.format("배치 크기가 %d에서 %d로 변경되었습니다", oldSize, batchSize);
        
        return ResponseEntity
            .ok(ApiResponse.success(message, message));
    }
}