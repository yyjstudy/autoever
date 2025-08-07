package com.autoever.member.controller;

import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.message.result.MessageSendTracker;
import com.autoever.member.message.service.BulkMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AdminMessageController 테스트")
@TestPropertySource(properties = {
    "jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHktMTIzNDU2Nzg5MA==",
    "jwt.expiration-time=3600",
    "async.enabled=false"
})
class AdminMessageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private BulkMessageService bulkMessageService;
    
    @MockBean
    private MessageSendTracker messageSendTracker;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("대량 메시지 발송 요청 - 성공")
    void sendBulkMessage_Success() throws Exception {
        // Given
        MessageSendDto request = new MessageSendDto("TWENTIES", "할인 쿠폰이 발급되었습니다!");
        UUID jobId = UUID.randomUUID();
        BulkMessageResponse response = BulkMessageResponse.inProgress(jobId, 15000);
        
        when(bulkMessageService.sendBulkMessage(any(MessageSendDto.class)))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/admin/messages/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("대량 메시지 발송이 시작되었습니다."))
            .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
            .andExpect(jsonPath("$.data.totalUsers").value(15000))
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("대량 메시지 발송 요청 - 유효성 검증 실패")
    void sendBulkMessage_ValidationFail() throws Exception {
        // Given
        MessageSendDto request = new MessageSendDto("INVALID_AGE_GROUP", "");
        
        // When & Then
        mockMvc.perform(post("/api/admin/messages/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("대량 메시지 발송 요청 - 권한 없음")
    void sendBulkMessage_Forbidden() throws Exception {
        // Given
        MessageSendDto request = new MessageSendDto("TWENTIES", "메시지");
        
        // When & Then
        mockMvc.perform(post("/api/admin/messages/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("작업 상태 조회 - 성공")
    void getJobStatus_Success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        BulkMessageJobStatus status = new BulkMessageJobStatus(
            jobId,
            BulkMessageResponse.JobStatus.COMPLETED,
            15000,
            15000,
            14950,
            50,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now(),
            Duration.ofMinutes(10),
            100.0
        );
        
        when(bulkMessageService.getJobStatus(jobId)).thenReturn(status);
        
        // When & Then
        mockMvc.perform(get("/api/admin/messages/send/{jobId}/status", jobId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("작업 상태 조회 성공"))
            .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.totalUsers").value(15000))
            .andExpect(jsonPath("$.data.successCount").value(14950));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자 - 401")
    void unauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("메시지 발송 통계 조회 - 성공")
    void getMessageStatistics_Success() throws Exception {
        // Given
        MessageSendTracker.SendStatistics statistics = new MessageSendTracker.SendStatistics(
            1523,
            94.7,
            23.1,
            1089,
            352,
            72,
            10,
            50,  // queuedCount
            1523,
            424,
            10,  // currentQueueSize
            1500  // maxQueueSize
        );
        
        when(messageSendTracker.getStatistics()).thenReturn(statistics);
        
        // When & Then
        mockMvc.perform(get("/api/admin/messages/statistics")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("메시지 발송 통계 조회가 완료되었습니다."))
            .andExpect(jsonPath("$.data.totalAttempts").value(1523))
            .andExpect(jsonPath("$.data.successRate").value(94.7))
            .andExpect(jsonPath("$.data.fallbackRate").value(23.1))
            .andExpect(jsonPath("$.data.kakaoSuccessCount").value(1089))
            .andExpect(jsonPath("$.data.smsFallbackCount").value(352))
            .andExpect(jsonPath("$.data.failedCount").value(72))
            .andExpect(jsonPath("$.data.rateLimitedCount").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("메시지 발송 통계 조회 - 권한 없음")
    void getMessageStatistics_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/messages/statistics")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("메시지 발송 통계 초기화 - 성공")
    void resetMessageStatistics_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/admin/messages/statistics/reset")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("메시지 발송 통계가 초기화되었습니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("메시지 발송 통계 초기화 - 권한 없음")
    void resetMessageStatistics_Forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/messages/statistics/reset")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }
}