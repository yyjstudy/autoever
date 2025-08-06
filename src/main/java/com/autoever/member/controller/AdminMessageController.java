package com.autoever.member.controller;

import com.autoever.member.dto.ApiResponse;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.message.service.BulkMessageService;
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
}