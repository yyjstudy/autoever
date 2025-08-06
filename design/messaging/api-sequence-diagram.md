# 외부 API 연동 시퀀스 다이어그램

## 1. 단일 메시지 발송 시퀀스 (정상 케이스)

```mermaid
sequenceDiagram
    participant AC as AdminController
    participant MS as MessageService
    participant MCF as MessageClientFactory
    participant RL as RateLimiter
    participant KTC as KakaoTalkClient
    participant KA as KakaoTalk API
    participant ML as MessageLogger
    
    AC->>MS: sendMessage(recipient, content)
    MS->>MCF: getAvailableClient(KAKAOTALK)
    MCF->>RL: tryAcquire(KAKAOTALK)
    RL-->>MCF: true (token acquired)
    MCF-->>MS: KakaoTalkClient
    
    MS->>KTC: sendMessage(recipient, content)
    KTC->>KTC: buildRequest(recipient, content)
    KTC->>KA: POST /kakaotalk-messages
    
    Note over KA: 외부 API 처리
    
    KA-->>KTC: 200 OK {messageId: "msg123"}
    KTC->>KTC: handleResponse(response)
    KTC-->>MS: MessageResponse(success=true)
    
    MS->>ML: logMessageSent(KAKAOTALK, success=true)
    MS-->>AC: success response
```

## 2. Rate Limiting으로 인한 대기 시퀀스

```mermaid
sequenceDiagram
    participant MS as MessageService
    participant MCF as MessageClientFactory
    participant RL as RateLimiter
    participant TB as TokenBucket
    participant KTC as KakaoTalkClient
    participant KA as KakaoTalk API
    
    MS->>MCF: getAvailableClient(KAKAOTALK)
    MCF->>RL: tryAcquire(KAKAOTALK)
    RL->>TB: tryConsume(1)
    TB-->>RL: false (no tokens available)
    RL-->>MCF: false (rate limited)
    
    Note over MCF: Wait for token refill
    
    MCF->>RL: tryAcquire(KAKAOTALK, 5000ms)
    
    loop Token Refill Check
        TB->>TB: refill()
        TB->>TB: check available tokens
    end
    
    RL->>TB: tryConsume(1)
    TB-->>RL: true (token available)
    RL-->>MCF: true (token acquired)
    
    MCF-->>MS: KakaoTalkClient
    MS->>KTC: sendMessage(recipient, content)
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: 200 OK
    KTC-->>MS: success
```

## 3. 재시도 메커니즘 시퀀스

```mermaid
sequenceDiagram
    participant MS as MessageService
    participant RM as RetryMechanism
    participant KTC as KakaoTalkClient
    participant KA as KakaoTalk API
    participant ML as MessageLogger
    
    MS->>RM: execute(() -> sendMessage())
    RM->>KTC: sendMessage(recipient, content) [Attempt 1]
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: 500 Internal Server Error
    KTC-->>RM: Exception: API_ERROR
    
    RM->>RM: shouldRetry(API_ERROR, attempt=1) → true
    RM->>RM: calculateDelay(1) → 1000ms
    RM->>RM: sleep(1000ms)
    
    RM->>KTC: sendMessage(recipient, content) [Attempt 2]
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: 429 Too Many Requests
    KTC-->>RM: Exception: RATE_LIMITED
    
    RM->>RM: shouldRetry(RATE_LIMITED, attempt=2) → true
    RM->>RM: calculateDelay(2) → 2000ms
    RM->>RM: sleep(2000ms)
    
    RM->>KTC: sendMessage(recipient, content) [Attempt 3]
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: 200 OK
    KTC-->>RM: success
    RM-->>MS: success
    
    MS->>ML: logRetrySuccess(attempts=3)
```

## 4. 대체 발송 (Fallback) 시퀀스

```mermaid
sequenceDiagram
    participant MS as MessageService
    participant RM as RetryMechanism
    participant KTC as KakaoTalkClient
    participant SAC as SmsClient
    participant KA as KakaoTalk API
    participant SA as SMS API
    participant ML as MessageLogger
    
    MS->>RM: executeWithFallback(primary, fallback)
    
    Note over RM: Primary attempt (KakaoTalk)
    RM->>KTC: sendMessage(recipient, content)
    
    loop 3 attempts
        KTC->>KA: POST /kakaotalk-messages
        KA-->>KTC: 503 Service Unavailable
        KTC-->>RM: Exception: SERVICE_UNAVAILABLE
        RM->>RM: retry with exponential backoff
    end
    
    Note over RM: All primary attempts failed
    RM->>ML: logFallbackTriggered(KAKAOTALK → SMS)
    
    Note over RM: Fallback attempt (SMS)
    RM->>SAC: sendMessage(recipient, content)
    SAC->>SA: POST /sms
    SA-->>SAC: 200 OK {smsId: "sms456"}
    SAC-->>RM: success
    
    RM-->>MS: fallback success
    MS->>ML: logFallbackSuccess(KAKAOTALK → SMS)
```

## 5. 대용량 메시지 발송 시퀀스

```mermaid
sequenceDiagram
    participant AC as AdminController
    participant MSS as MessageSendService
    participant ACS as AgeCalculationService
    participant UQS as UserQueryService
    participant AMPS as AsyncMessageProcessingService
    participant TE as ThreadExecutor
    participant MCF as MessageClientFactory
    participant API as External APIs
    
    AC->>MSS: sendBulkMessage(ageGroup, message)
    MSS->>ACS: getAgeRange(ageGroup)
    ACS-->>MSS: AgeRange(20, 29)
    
    MSS->>UQS: countUsersByAgeRange(ageRange)
    UQS-->>MSS: totalUsers = 15420
    
    MSS->>MSS: createJob(jobId, totalUsers)
    MSS-->>AC: jobId, status=IN_PROGRESS
    
    Note over MSS: 비동기 처리 시작
    
    par Batch Processing
        MSS->>UQS: getUsersBatch(ageRange, page=1, size=1000)
        UQS-->>MSS: users[0-999]
        MSS->>AMPS: processUserBatch(users, message, jobId)
        
        AMPS->>TE: submitAsync(sendTask1)
        TE->>MCF: getAvailableClient()
        MCF-->>TE: KakaoTalkClient
        TE->>API: send messages[0-99]
        API-->>TE: results[0-99]
        
    and
        MSS->>UQS: getUsersBatch(ageRange, page=2, size=1000)
        UQS-->>MSS: users[1000-1999]
        MSS->>AMPS: processUserBatch(users, message, jobId)
        
        AMPS->>TE: submitAsync(sendTask2)
        TE->>MCF: getAvailableClient()
        MCF-->>TE: SmsClient
        TE->>API: send messages[1000-1099]
        API-->>TE: results[1000-1099]
        
    and
        Note over MSS: ... 더 많은 배치들 ...
    end
    
    Note over AMPS: 모든 배치 완료 대기
    AMPS-->>MSS: allBatchesComplete()
    MSS->>MSS: updateJobStatus(COMPLETED)
```

## 6. 에러 처리 및 복구 시퀀스

```mermaid
sequenceDiagram
    participant MS as MessageService
    participant CB as CircuitBreaker
    participant KTC as KakaoTalkClient
    participant KA as KakaoTalk API
    participant SAC as SmsClient
    participant HS as HealthChecker
    participant AS as AlertSystem
    
    MS->>CB: call(() -> sendMessage())
    CB->>KTC: sendMessage(recipient, content)
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: Connection Timeout
    KTC-->>CB: Exception: TIMEOUT
    CB->>CB: recordFailure()
    
    Note over CB: 실패 횟수 증가
    
    CB-->>MS: Exception: TIMEOUT
    MS->>MS: handleError(TIMEOUT)
    
    loop 5 consecutive failures
        MS->>CB: call(() -> sendMessage())
        CB->>CB: checkState() → CLOSED
        CB->>KTC: sendMessage()
        KTC->>KA: POST /kakaotalk-messages
        KA-->>KTC: Various errors
        KTC-->>CB: Exception
        CB->>CB: recordFailure()
    end
    
    CB->>CB: tripBreaker() → OPEN state
    CB->>HS: checkHealth(KAKAOTALK)
    HS->>AS: alertServiceDown(KAKAOTALK)
    
    Note over CB: Circuit Breaker OPEN
    
    MS->>CB: call(() -> sendMessage())
    CB->>CB: checkState() → OPEN
    CB-->>MS: CircuitBreakerOpenException
    
    MS->>SAC: sendMessage() [fallback to SMS]
    SAC-->>MS: success
    
    Note over CB: After timeout period
    
    CB->>CB: checkState() → HALF_OPEN
    MS->>CB: call(() -> sendMessage())
    CB->>KTC: sendMessage() [test call]
    KTC->>KA: POST /kakaotalk-messages
    KA-->>KTC: 200 OK [service recovered]
    KTC-->>CB: success
    CB->>CB: recordSuccess() → CLOSED state
    CB-->>MS: success
    
    CB->>AS: alertServiceRecovered(KAKAOTALK)
```

## 7. 메시지 발송 모니터링 시퀀스

```mermaid
sequenceDiagram
    participant KTC as KakaoTalkClient
    participant SAC as SmsClient
    participant MSM as MessageSendMonitor
    participant MR as MeterRegistry
    participant ML as MetricsLogger
    participant AD as AlertingDashboard
    
    par KakaoTalk Monitoring
        KTC->>MSM: recordMessageSent(KAKAOTALK, success=true)
        MSM->>MR: increment(kakaotalk_messages_sent_total)
        MSM->>MR: record(kakaotalk_response_time, 250ms)
        
    and SMS Monitoring
        SAC->>MSM: recordMessageSent(SMS, success=false)
        MSM->>MR: increment(sms_messages_failed_total)
        MSM->>MR: record(sms_response_time, 1200ms)
    end
    
    MSM->>ML: logMetrics(timestamp, metrics)
    
    loop Every 30 seconds
        MSM->>MSM: calculateMetrics()
        MSM->>MR: gauge(success_rate, 0.95)
        MSM->>MR: gauge(active_jobs, 5)
        MSM->>MR: gauge(queue_size, 1250)
    end
    
    Note over MSM: Threshold check
    MSM->>MSM: checkThresholds()
    
    alt Success Rate < 90%
        MSM->>AD: alert(LOW_SUCCESS_RATE, 85%)
    else Queue Size > 5000
        MSM->>AD: alert(HIGH_QUEUE_SIZE, 5200)
    else Response Time > 5s
        MSM->>AD: alert(HIGH_RESPONSE_TIME, 6500ms)
    end
```

## 8. 설정 및 초기화 시퀀스

```mermaid
sequenceDiagram
    participant App as Application
    participant Config as Configuration
    participant MCF as MessageClientFactory
    participant KTC as KakaoTalkClient
    participant SAC as SmsClient
    participant RL as RateLimiter
    participant TE as ThreadExecutor
    
    App->>Config: loadConfiguration()
    Config-->>App: MessageApiConfig
    
    App->>MCF: initialize(config)
    
    par Client Initialization
        MCF->>KTC: new KakaoTalkClient(kakaoConfig)
        KTC->>KTC: validateConfiguration()
        KTC->>KTC: initializeRestTemplate()
        KTC->>KTC: setupBasicAuth()
        
    and
        MCF->>SAC: new SmsClient(smsConfig)
        SAC->>SAC: validateConfiguration()
        SAC->>SAC: initializeRestTemplate()
        SAC->>SAC: setupBasicAuth()
    end
    
    MCF->>RL: initializeRateLimiters(config)
    RL->>RL: createTokenBucket(KAKAOTALK, 100/min)
    RL->>RL: createTokenBucket(SMS, 500/min)
    RL->>RL: scheduleRefillTask()
    
    MCF->>TE: initializeThreadPool(config)
    TE->>TE: setCorePoolSize(10)
    TE->>TE: setMaxPoolSize(50)
    TE->>TE: setQueueCapacity(1000)
    
    par Health Checks
        MCF->>KTC: healthCheck()
        KTC-->>MCF: healthy
        
    and
        MCF->>SAC: healthCheck()
        SAC-->>MCF: healthy
    end
    
    MCF-->>App: initialization complete
```

이러한 시퀀스 다이어그램들은 Task 9와 Task 10의 구현 시 참고할 수 있는 상세한 상호작용 플로우를 제공하며, 각종 예외 상황과 복구 메커니즘까지 포함하고 있습니다.