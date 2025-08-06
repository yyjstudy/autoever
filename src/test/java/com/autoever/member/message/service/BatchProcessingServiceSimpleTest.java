package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeGroup;
import com.autoever.member.message.dto.AgeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchProcessingService Simple 테스트")
class BatchProcessingServiceSimpleTest {
    
    @Mock
    private UserQueryService userQueryService;
    
    @Mock
    private AgeCalculationService ageCalculationService;
    
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;
    
    @InjectMocks
    private BatchProcessingService batchProcessingService;
    
    @Test
    @DisplayName("스레드 풀 상태 조회")
    void getThreadPoolStatus() {
        // Given
        ThreadPoolExecutor mockExecutor = mock(ThreadPoolExecutor.class);
        when(taskExecutor.getCorePoolSize()).thenReturn(10);
        when(taskExecutor.getMaxPoolSize()).thenReturn(50);
        when(taskExecutor.getActiveCount()).thenReturn(5);
        when(taskExecutor.getQueueSize()).thenReturn(20);
        when(taskExecutor.getQueueCapacity()).thenReturn(1000);
        when(taskExecutor.getPoolSize()).thenReturn(10);
        when(taskExecutor.getThreadPoolExecutor()).thenReturn(mockExecutor);
        when(mockExecutor.getCompletedTaskCount()).thenReturn(100L);
        
        // When
        BatchProcessingService.ThreadPoolStatus status = batchProcessingService.getThreadPoolStatus();
        
        // Then
        assertThat(status.corePoolSize()).isEqualTo(10);
        assertThat(status.maxPoolSize()).isEqualTo(50);
        assertThat(status.activeCount()).isEqualTo(5);
        assertThat(status.queueSize()).isEqualTo(20);
        assertThat(status.completedTaskCount()).isEqualTo(100L);
        assertThat(status.getUtilization()).isEqualTo(10.0); // 5/50 * 100
        assertThat(status.getQueueUtilization()).isEqualTo(2.0); // 20/1000 * 100
    }
    
    @Test
    @DisplayName("배치 진행률 계산")
    void batchProgressCalculation() {
        // Given
        UUID jobId = UUID.randomUUID();
        BatchProcessingService.BatchProgress progress = new BatchProcessingService.BatchProgress(
            jobId, 1000, 250, 240, 10
        );
        
        // Then
        assertThat(progress.getProgressPercentage()).isEqualTo(25.0);
        assertThat(progress.getSuccessRate()).isEqualTo(96.0);
    }
    
    @Test
    @DisplayName("스레드 풀 유틸리제이션 계산")
    void threadPoolUtilization() {
        // Given
        BatchProcessingService.ThreadPoolStatus status = new BatchProcessingService.ThreadPoolStatus(
            10, 50, 25, 200, 1000, 30, 1500L
        );
        
        // Then
        assertThat(status.getUtilization()).isEqualTo(50.0); // 25/50 * 100
        assertThat(status.getQueueUtilization()).isEqualTo(20.0); // 200/1000 * 100
    }
}