package com.autoever.member.message.queue;

import com.autoever.member.message.ApiType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 메시지 큐 기능 튜토리얼 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("메시지 큐 튜토리얼 테스트")
class MessageQueueTutorialTest {
    
    @Test
    @DisplayName("큐 기본 동작 확인")
    void testQueueBasicOperations() {
        MessageQueueService queueService = new MessageQueueService();
        
        System.out.println("=== 메시지 큐 튜토리얼 ===");
        
        // 1. 초기 상태 확인
        MessageQueueService.QueueStatus status = queueService.getQueueStatus();
        System.out.println("초기 큐 상태: " + status.getCurrentSize() + "/" + status.getMaxSize());
        assertThat(status.getCurrentSize()).isEqualTo(0);
        assertThat(status.getMaxSize()).isEqualTo(1500);
        
        // 2. 정상 추가 테스트
        MessageQueueService.QueueResult result1 = queueService.enqueue(
            "김철수", "010-1234-5678", "테스트 메시지 1", ApiType.KAKAOTALK);
        
        System.out.println("첫 번째 메시지 추가: " + result1.isSuccess() + " - " + result1.getMessage());
        assertThat(result1.isSuccess()).isTrue();
        
        // 3. 여러 메시지 추가 (10개 정도만)
        System.out.println("\n=== 소량 메시지 추가 테스트 ===");
        int successCount = 1; // 이미 1개 추가됨
        
        for (int i = 2; i <= 10; i++) {
            MessageQueueService.QueueResult result = queueService.enqueue(
                "사용자" + i, "010-1234-567" + (i % 10), "테스트 메시지 " + i, 
                (i % 2 == 0) ? ApiType.SMS : ApiType.KAKAOTALK);
            
            if (result.isSuccess()) {
                successCount++;
                System.out.println(i + "번째 메시지 추가 성공 - 큐 위치: " + result.getQueuePosition());
            }
        }
        
        System.out.println("총 추가된 메시지: " + successCount + "개");
        
        // 4. 현재 상태 확인
        MessageQueueService.QueueStatus currentStatus = queueService.getQueueStatus();
        System.out.println("현재 큐 상태: " + currentStatus.getCurrentSize() + "/" + currentStatus.getMaxSize() 
            + " (" + String.format("%.1f", currentStatus.getUsagePercent()) + "%)");
        
        assertThat(currentStatus.getCurrentSize()).isEqualTo(10);
        assertThat(currentStatus.isFull()).isFalse();
        
        // 5. 큐에서 메시지 꺼내기 테스트
        System.out.println("\n=== 큐에서 메시지 처리 테스트 ===");
        
        for (int i = 0; i < 3; i++) {
            MessageQueueItem item = queueService.dequeue();
            if (item != null) {
                System.out.println("처리: " + item.getMemberName() + " - " + item.getPreferredApiType());
            }
        }
        
        MessageQueueService.QueueStatus afterDequeue = queueService.getQueueStatus();
        System.out.println("3개 처리 후 큐 상태: " + afterDequeue.getCurrentSize() + "/" + afterDequeue.getMaxSize());
        
        assertThat(afterDequeue.getCurrentSize()).isEqualTo(7); // 10 - 3 = 7
        
        System.out.println("\n✅ 큐 기본 동작 테스트 완료!");
    }
    
    @Test 
    @DisplayName("Rate Limit + Queue 시나리오 테스트")
    void testRateLimitWithQueueScenario() {
        System.out.println("\n=== Rate Limit + Queue 시나리오 ===");
        System.out.println("현재 Rate Limit: KAKAOTALK(2/분), SMS(10/분) → 총 12/분");
        System.out.println("100명의 사용자가 동시 요청 시나리오");
        
        MessageQueueService queueService = new MessageQueueService();
        
        int totalUsers = 100;
        int successCount = 0;
        int queueFullCount = 0;
        
        // 100명이 동시 요청하는 상황 시뮬레이션
        for (int i = 1; i <= totalUsers; i++) {
            MessageQueueService.QueueResult result = queueService.enqueue(
                "사용자" + i, "010-" + String.format("%04d", i) + "-5678", 
                "긴급 메시지 " + i, (i <= 50) ? ApiType.KAKAOTALK : ApiType.SMS);
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                queueFullCount++;
                if (queueFullCount == 1) {
                    System.out.println("첫 번째 실패 사용자 (" + i + "번째): " + result.getMessage());
                }
            }
        }
        
        System.out.println("\n📊 결과 분석:");
        System.out.println("✅ 큐에 추가된 사용자: " + successCount + "명");
        System.out.println("❌ 큐 가득참으로 실패한 사용자: " + queueFullCount + "명");
        
        // 현재 1500 큐 사이즈이므로 100명은 모두 큐에 들어갈 수 있어야 함
        assertThat(successCount).isEqualTo(100);
        assertThat(queueFullCount).isEqualTo(0);
        
        System.out.println("\n💡 분석:");
        System.out.println("- Rate Limit으로 즉시 처리 불가능한 88명이 큐에서 대기");
        System.out.println("- 백그라운드 프로세서가 5초마다 큐를 확인하여 순차 처리");
        System.out.println("- 사용자들은 '대기열에 추가됨' 상태를 받고 순서대로 처리됨");
        
        System.out.println("\n✅ Rate Limit + Queue 시나리오 테스트 완료!");
    }
    
    @Test
    @DisplayName("큐 오버플로우 극한 시나리오")
    void testQueueOverflowExtremeScenario() {
        System.out.println("\n=== 큐 오버플로우 극한 시나리오 ===");
        System.out.println("2000명이 동시 요청하는 극한 상황 (큐 MAX: 1500)");
        
        MessageQueueService queueService = new MessageQueueService();
        
        int totalUsers = 2000;
        int successCount = 0;
        int queueFullCount = 0;
        
        for (int i = 1; i <= totalUsers; i++) {
            MessageQueueService.QueueResult result = queueService.enqueue(
                "사용자" + i, "010-" + String.format("%04d", i % 10000) + "-5678", 
                "긴급 메시지 " + i, (i % 3 == 0) ? ApiType.SMS : ApiType.KAKAOTALK);
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                queueFullCount++;
                if (queueFullCount == 1) {
                    System.out.println("큐 가득참! " + i + "번째 사용자부터 실패: " + result.getMessage());
                }
            }
            
            // 진행 상황 출력 (500명마다)
            if (i % 500 == 0) {
                System.out.println(i + "명 처리 완료 - 성공: " + successCount + ", 실패: " + queueFullCount);
            }
        }
        
        System.out.println("\n📊 최종 결과:");
        System.out.println("✅ 큐에 추가된 사용자: " + successCount + "명");
        System.out.println("❌ 큐 가득참으로 실패한 사용자: " + queueFullCount + "명");
        System.out.println("성공률: " + String.format("%.1f", (double) successCount / totalUsers * 100) + "%");
        
        assertThat(successCount).isEqualTo(1500); // MAX_QUEUE_SIZE
        assertThat(queueFullCount).isEqualTo(500); // 2000 - 1500 = 500
        
        System.out.println("\n💡 실제 운영 시뮬레이션:");
        System.out.println("- 1500명: '대기열에 추가됨 - 순서대로 처리 예정' 응답");
        System.out.println("- 500명: '대기열이 가득참 - 나중에 다시 시도해주세요' 응답");
        System.out.println("- 대기열의 메시지들은 백그라운드에서 순차 처리됨");
        
        System.out.println("\n✅ 극한 시나리오 테스트 완료!");
    }
}