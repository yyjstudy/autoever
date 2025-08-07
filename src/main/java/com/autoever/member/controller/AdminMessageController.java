package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.service.BulkMessageService;
import com.autoever.member.message.service.MessagePerformanceService;
import com.autoever.member.message.service.DynamicBatchOptimizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
    private final MessageSendTracker messageSendTracker;
    
    /**
     * 연령대별 대량 메시지 발송
     */
    @PostMapping("/send")
    @Operation(
        summary = "연령대별 대량 메시지 발송", 
        description = """
            특정 연령대의 모든 회원에게 메시지를 발송합니다.
            
            **연령대 분류:**
            - TEENS: 10대 (10~19세)
            - TWENTIES: 20대 (20~29세)  
            - THIRTIES: 30대 (30~39세)
            - FORTIES: 40대 (40~49세)
            - FIFTIES_PLUS: 50대 이상 (50세~)
            
            **메시지 템플릿:**
            입력한 메시지 앞에 '{회원 성명}님, 안녕하세요. 현대 오토에버입니다.' 템플릿이 자동으로 추가됩니다.
            """)
    @RequestBody(
        description = "대량 메시지 발송 요청 정보",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MessageSendDto.class),
            examples = {
                @ExampleObject(
                    name = "20대 대상 할인 쿠폰 발송",
                    description = "20대 회원들에게 할인 쿠폰 안내 메시지 발송",
                    value = """
                        {
                          "ageGroup": "TWENTIES",
                          "message": "신제품 출시 기념 20% 할인 쿠폰이 발급되었습니다!"
                        }
                        """
                ),
                @ExampleObject(
                    name = "30대 대상 이벤트 안내",
                    description = "30대 회원들에게 특별 이벤트 안내",
                    value = """
                        {
                          "ageGroup": "THIRTIES",
                          "message": "가족과 함께하는 특별 이벤트에 참여하세요. 추가 혜택이 준비되어 있습니다."
                        }
                        """
                ),
                @ExampleObject(
                    name = "50대 이상 대상 건강 관련 안내",
                    description = "50대 이상 회원들에게 건강 관련 서비스 안내",
                    value = """
                        {
                          "ageGroup": "FIFTIES_PLUS",
                          "message": "건강한 라이프스타일을 위한 맞춤형 서비스를 확인해보세요."
                        }
                        """
                )
            }
        )
    )
    public ResponseEntity<ApiResponse<BulkMessageResponse>> sendBulkMessage(
            @Valid @org.springframework.web.bind.annotation.RequestBody MessageSendDto request) {
        
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

    /**
     * 메시지 발송 통계 조회 API
     * 
     * @return 메시지 발송 통계 정보
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "메시지 발송 통계 조회",
        description = """
            관리자 권한으로 메시지 발송 통계를 조회합니다.
            
            **제공되는 정보**:
            - 전체 발송 시도 수
            - 성공률 (%)
            - Fallback 발생률 (%)
            - 채널별 성공/실패 건수
            - Rate Limiting 발생 건수
            
            **실시간 통계**: 시스템 시작 이후 누적 데이터를 제공합니다.
            """
    )
    public ResponseEntity<ApiResponse<MessageSendTracker.SendStatistics>> getMessageStatistics() {
        MessageSendTracker.SendStatistics statistics = messageSendTracker.getStatistics();
        
        return ResponseEntity.ok(ApiResponse.success(
            "메시지 발송 통계 조회가 완료되었습니다.", 
            statistics
        ));
    }

    /**
     * 메시지 발송 통계 초기화 API
     * 
     * @return 초기화 완료 메시지
     */
    @PostMapping("/statistics/reset")
    @Operation(
        summary = "메시지 발송 통계 초기화",
        description = """
            관리자 권한으로 메시지 발송 통계를 초기화합니다.
            
            **주의사항**:
            - 모든 누적 통계가 0으로 재설정됩니다
            - 이 작업은 되돌릴 수 없습니다
            - 시스템 성능에는 영향을 주지 않습니다
            """
    )
    public ResponseEntity<ApiResponse<Void>> resetMessageStatistics() {
        messageSendTracker.reset();
        
        return ResponseEntity.ok(ApiResponse.success(
            "메시지 발송 통계가 초기화되었습니다.", 
            null
        ));
    }
}