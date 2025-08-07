package com.autoever.member.message.queue;

import com.autoever.member.message.ApiType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 메시지 큐 빠른 테스트
 */
@DisplayName("메시지 큐 빠른 테스트")
class MessageQueueQuickTest {
    
    @Test
    @DisplayName("큐 기본 기능 검증")
    void testQueueBasicFunctionality() {
        MessageQueueService queueService = new MessageQueueService();
        
        // 1. 초기 상태
        MessageQueueService.QueueStatus initialStatus = queueService.getQueueStatus();
        assertThat(initialStatus.getCurrentSize()).isEqualTo(0);
        assertThat(initialStatus.getMaxSize()).isEqualTo(1500);
        assertThat(initialStatus.isFull()).isFalse();
        
        // 2. 메시지 추가
        MessageQueueService.QueueResult result = queueService.enqueue(
            "김철수", "010-1234-5678", "테스트 메시지", ApiType.KAKAOTALK);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getQueueId()).isNotNull();
        assertThat(result.getQueuePosition()).isEqualTo(1);
        assertThat(result.getMessage()).contains("대기열에 추가");
        
        // 3. 큐 상태 변화 확인
        MessageQueueService.QueueStatus afterAdd = queueService.getQueueStatus();
        assertThat(afterAdd.getCurrentSize()).isEqualTo(1);
        assertThat(afterAdd.getUsagePercent()).isCloseTo(0.067, within(0.01)); // 1/1500 * 100
        
        // 4. 메시지 꺼내기
        MessageQueueItem item = queueService.dequeue();
        assertThat(item).isNotNull();
        assertThat(item.getMemberName()).isEqualTo("김철수");
        assertThat(item.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(item.getPreferredApiType()).isEqualTo(ApiType.KAKAOTALK);
        
        // 5. 빈 큐 상태
        MessageQueueService.QueueStatus emptyStatus = queueService.getQueueStatus();
        assertThat(emptyStatus.getCurrentSize()).isEqualTo(0);
        
        MessageQueueItem emptyItem = queueService.dequeue();
        assertThat(emptyItem).isNull();
    }
    
    @Test
    @DisplayName("100명 사용자 시나리오")
    void testHundredUsersScenario() {
        MessageQueueService queueService = new MessageQueueService();
        
        int totalUsers = 100;
        int successCount = 0;
        int failureCount = 0;
        
        // 100명이 메시지 요청
        for (int i = 1; i <= totalUsers; i++) {
            MessageQueueService.QueueResult result = queueService.enqueue(
                "사용자" + i, 
                "010-" + String.format("%04d", i) + "-5678",
                "긴급 메시지 " + i,
                (i % 2 == 0) ? ApiType.SMS : ApiType.KAKAOTALK
            );
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        // 검증: 100명 모두 큐에 들어가야 함 (1500 > 100)
        assertThat(successCount).isEqualTo(100);
        assertThat(failureCount).isEqualTo(0);
        
        MessageQueueService.QueueStatus status = queueService.getQueueStatus();
        assertThat(status.getCurrentSize()).isEqualTo(100);
        assertThat(status.getUsagePercent()).isCloseTo(6.67, within(0.1)); // 100/1500 * 100
        
        // 처리 시뮬레이션: 12명만 처리 (Rate limit)
        int processedCount = 0;
        for (int i = 0; i < 12; i++) { // KAKAOTALK 2 + SMS 10 = 12
            MessageQueueItem item = queueService.dequeue();
            if (item != null) {
                processedCount++;
            }
        }
        
        assertThat(processedCount).isEqualTo(12);
        
        MessageQueueService.QueueStatus afterProcess = queueService.getQueueStatus();
        assertThat(afterProcess.getCurrentSize()).isEqualTo(88); // 100 - 12 = 88 (대기 중)
    }
    
    @Test 
    @DisplayName("큐 오버플로우 시나리오 (2000명)")
    void testQueueOverflowScenario() {
        MessageQueueService queueService = new MessageQueueService();
        
        int totalUsers = 2000;
        int successCount = 0;
        int failureCount = 0;
        
        // 2000명이 메시지 요청
        for (int i = 1; i <= totalUsers; i++) {
            MessageQueueService.QueueResult result = queueService.enqueue(
                "사용자" + i,
                "010-" + String.format("%04d", i % 10000) + "-5678", 
                "긴급 알림 " + i,
                ApiType.KAKAOTALK
            );
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // 첫 번째 실패 케이스 확인
                if (failureCount == 1) {
                    assertThat(result.getMessage()).contains("대기열이 가득참");
                    assertThat(result.getMessage()).contains("나중에 다시 시도");
                }
            }
        }
        
        // 검증
        assertThat(successCount).isEqualTo(1500); // MAX_QUEUE_SIZE
        assertThat(failureCount).isEqualTo(500);  // 2000 - 1500
        
        MessageQueueService.QueueStatus status = queueService.getQueueStatus();
        assertThat(status.getCurrentSize()).isEqualTo(1500);
        assertThat(status.isFull()).isTrue();
        assertThat(status.getUsagePercent()).isEqualTo(100.0);
        
        // 추가 요청 시도 (실패해야 함)
        MessageQueueService.QueueResult overflowResult = queueService.enqueue(
            "오버플로우테스트", "010-9999-9999", "오버플로우 메시지", ApiType.SMS);
        
        assertThat(overflowResult.isSuccess()).isFalse();
        assertThat(overflowResult.getMessage()).contains("대기열이 가득참");
    }
}