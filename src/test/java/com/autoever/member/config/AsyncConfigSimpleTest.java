package com.autoever.member.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AsyncConfig Simple 테스트")
class AsyncConfigSimpleTest {
    
    @Test
    @DisplayName("AsyncConfig 생성")
    void asyncConfigCreation() {
        AsyncConfig asyncConfig = new AsyncConfig();
        assertThat(asyncConfig).isNotNull();
    }
    
    @Test
    @DisplayName("ThreadPoolTaskExecutor 생성")
    void threadPoolTaskExecutorCreation() {
        // Given
        AsyncConfig asyncConfig = new AsyncConfig();
        // @Value 필드 설정
        ReflectionTestUtils.setField(asyncConfig, "corePoolSize", 10);
        ReflectionTestUtils.setField(asyncConfig, "maxPoolSize", 50);
        ReflectionTestUtils.setField(asyncConfig, "queueCapacity", 1000);
        ReflectionTestUtils.setField(asyncConfig, "keepAliveSeconds", 60);
        ReflectionTestUtils.setField(asyncConfig, "threadNamePrefix", "test-");
        ReflectionTestUtils.setField(asyncConfig, "awaitTerminationSeconds", 30);
        
        // When
        Object executor = asyncConfig.getAsyncExecutor();
        
        // Then
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(10);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(50);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(1000);
    }
    
    @Test
    @DisplayName("예외 처리기")
    void exceptionHandler() {
        AsyncConfig asyncConfig = new AsyncConfig();
        assertThat(asyncConfig.getAsyncUncaughtExceptionHandler()).isNotNull();
    }
}