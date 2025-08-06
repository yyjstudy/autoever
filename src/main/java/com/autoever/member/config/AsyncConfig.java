package com.autoever.member.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig implements AsyncConfigurer {
    
    @Value("${async.thread-pool.core-size:10}")
    private int corePoolSize;
    
    @Value("${async.thread-pool.max-size:50}")
    private int maxPoolSize;
    
    @Value("${async.thread-pool.queue-capacity:1000}")
    private int queueCapacity;
    
    @Value("${async.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;
    
    @Value("${async.thread-pool.thread-name-prefix:message-sender-}")
    private String threadNamePrefix;
    
    @Value("${async.thread-pool.await-termination-seconds:30}")
    private int awaitTerminationSeconds;
    
    /**
     * 메시지 발송용 ThreadPoolTaskExecutor
     */
    @Bean(name = "messageTaskExecutor")
    public ThreadPoolTaskExecutor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 풀 설정
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // 거부 정책: 호출자 스레드에서 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 종료 시 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        // 스레드 풀 통계 로깅
        executor.setTaskDecorator(runnable -> {
            return () -> {
                long startTime = System.currentTimeMillis();
                try {
                    runnable.run();
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("작업 실행 시간: {}ms, 활성 스레드: {}, 큐 크기: {}", 
                            duration, 
                            executor.getActiveCount(), 
                            executor.getQueueSize());
                }
            };
        });
        
        executor.initialize();
        
        log.info("메시지 발송 ThreadPoolTaskExecutor 초기화 완료 - " +
                "코어: {}, 최대: {}, 큐: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
    
    /**
     * 기본 비동기 실행자
     */
    @Override
    public Executor getAsyncExecutor() {
        return messageTaskExecutor();
    }
    
    /**
     * 비동기 예외 처리기
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("비동기 메서드 실행 중 예외 발생 - 메서드: {}, 파라미터: {}", 
                    method.getName(), params, throwable);
        };
    }
}