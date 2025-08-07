package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.BulkMessageJobStatus;
import com.autoever.member.message.dto.BulkMessageResponse;
import com.autoever.member.message.dto.MessageSendDto;
import com.autoever.member.service.ExternalMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkMessageService 테스트")
class BulkMessageServiceTest {
    
    @Mock
    private AgeCalculationService ageCalculationService;
    
    @Mock
    private UserQueryService userQueryService;
    
    @Mock
    private BatchProcessingService batchProcessingService;
    
    @Mock
    private ExternalMessageService externalMessageService;
    
    
    @Mock
    private StructuredMessageLogger structuredLogger;
    
    @InjectMocks
    private BulkMessageService bulkMessageService;
    
    @BeforeEach
    void setUp() {
        
        // structuredLogger void 메서드들을 모킹
        lenient().doNothing().when(structuredLogger).logJobStart(any(UUID.class), anyString(), anyString(), anyInt());
        lenient().doNothing().when(structuredLogger).logBatchProcessing(any(UUID.class), anyInt(), anyInt(), anyLong(), anyInt(), anyInt());
        lenient().doNothing().when(structuredLogger).logJobCompletion(any(UUID.class), anyString(), anyInt(), anyInt(), anyInt(), anyLong());
        lenient().doNothing().when(structuredLogger).logMessageFailure(any(UUID.class), anyString(), anyString(), anyLong());
    }
    
    @Test
    @DisplayName("대량 메시지 발송 요청 - 성공적으로 시작")
    void sendBulkMessage_Success() {
        // Given
        MessageSendDto request = new MessageSendDto("TWENTIES", "할인 쿠폰 발급!");
        when(userQueryService.countUsersByAgeGroup(AgeGroup.TWENTIES)).thenReturn(100);
        
        // When
        BulkMessageResponse response = bulkMessageService.sendBulkMessage(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.jobId()).isNotNull();
        assertThat(response.totalUsers()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(BulkMessageResponse.JobStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isNotNull();
        assertThat(response.estimatedDuration()).isNotNull();
        
        // 작업 상태가 초기화되었는지 확인
        BulkMessageJobStatus status = bulkMessageService.getJobStatus(response.jobId());
        assertThat(status.jobId()).isEqualTo(response.jobId());
        assertThat(status.totalUsers()).isEqualTo(100);
        assertThat(status.status()).isIn(
            BulkMessageResponse.JobStatus.IN_PROGRESS,
            BulkMessageResponse.JobStatus.PROCESSING_BATCH,
            BulkMessageResponse.JobStatus.COMPLETED
        );
    }
    
    @Test
    @DisplayName("해당 연령대 사용자가 없는 경우")
    void sendBulkMessage_NoUsers() {
        // Given
        MessageSendDto request = new MessageSendDto("TEENS", "이벤트 알림");
        when(userQueryService.countUsersByAgeGroup(AgeGroup.TEENS)).thenReturn(0);
        
        // When
        BulkMessageResponse response = bulkMessageService.sendBulkMessage(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.jobId()).isNotNull();
        assertThat(response.totalUsers()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(BulkMessageResponse.JobStatus.COMPLETED);
        
        // 작업 상태 확인
        BulkMessageJobStatus status = bulkMessageService.getJobStatus(response.jobId());
        assertThat(status.status()).isEqualTo(BulkMessageResponse.JobStatus.COMPLETED);
        assertThat(status.progressPercentage()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("작업 상태 조회 - 존재하지 않는 작업 ID")
    void getJobStatus_NonExistentJobId() {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> bulkMessageService.getJobStatus(nonExistentJobId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("존재하지 않는 작업 ID입니다: " + nonExistentJobId);
    }
    
    @Test
    @DisplayName("비동기 메시지 발송 처리 - CompletableFuture 반환")
    void processMessageSendingAsync() {
        // Given
        UUID jobId = UUID.randomUUID();
        AgeGroup ageGroup = AgeGroup.TWENTIES;
        String message = "테스트 메시지";
        
        // When
        CompletableFuture<Void> future = bulkMessageService.processMessageSendingAsync(jobId, ageGroup, message);
        
        // Then - 단순히 CompletableFuture가 반환되는지만 확인
        assertThat(future).isNotNull();
    }
    
    private User createMockUser(String username, String phoneNumber) {
        return User.builder()
                .username(username)
                .password("password") // 필수 필드
                .name("테스트사용자")
                .socialNumber("900101-1234567") // 필수 필드
                .phoneNumber(phoneNumber)
                .email(username + "@test.com")
                .address("서울시") // 필수 필드
                .build();
    }
}