# Task 10: 대용량 메시지 발송 시스템 아키텍처

## 시스템 개요
연령대별 전체 회원 대상 메시지 발송 기능을 비동기 처리와 함께 구현하는 고성능 시스템

## 전체 시스템 아키텍처

```mermaid
graph TB
    subgraph "Web Layer"
        AC[AdminController] --> MSS[MessageSendService]
    end
    
    subgraph "Service Layer"
        MSS --> ACS[AgeCalculationService]
        MSS --> UQS[UserQueryService]
        MSS --> AMPS[AsyncMessageProcessingService]
    end
    
    subgraph "Async Processing Layer"
        AMPS --> TE[ThreadPoolTaskExecutor]
        TE --> CF1[CompletableFuture 1]
        TE --> CF2[CompletableFuture 2]
        TE --> CF3[CompletableFuture N]
    end
    
    subgraph "Message Client Layer"
        CF1 --> MAC[MessageApiClient]
        CF2 --> MAC
        CF3 --> MAC
        MAC --> EMS[External Message Services]
    end
    
    subgraph "Data Layer"
        UQS --> UR[UserRepository]
        MSS --> MST[MessageStatusTracker]
        MST --> MSD[MessageSendDatabase]
    end
    
    subgraph "Monitoring Layer"
        AMPS --> MSM[MessageSendMonitor]
        MSM --> ML[MetricsLogger]
        MSM --> AS[AlertSystem]
    end
    
    style TE fill:#e1f5fe
    style MAC fill:#e8f5e8
    style MST fill:#fff3e0
    style MSM fill:#fce4ec
```

## 비동기 처리 아키텍처

```mermaid
graph LR
    subgraph "Request Processing"
        REQ[Request] --> VAL[Validation]
        VAL --> AGE[Age Filtering]
        AGE --> BATCH[Batch Creation]
    end
    
    subgraph "Async Processing Pipeline"
        BATCH --> Q1[Queue 1]
        BATCH --> Q2[Queue 2]
        BATCH --> Q3[Queue N]
        
        Q1 --> W1[Worker 1]
        Q2 --> W2[Worker 2]
        Q3 --> WN[Worker N]
    end
    
    subgraph "Message Sending"
        W1 --> MS1[Message Send]
        W2 --> MS2[Message Send]
        WN --> MSN[Message Send]
    end
    
    subgraph "Result Aggregation"
        MS1 --> RA[Result Aggregator]
        MS2 --> RA
        MSN --> RA
        RA --> FR[Final Response]
    end
```

## 데이터 처리 플로우

```mermaid
sequenceDiagram
    participant AC as AdminController
    participant MSS as MessageSendService
    participant ACS as AgeCalculationService
    participant UQS as UserQueryService
    participant AMPS as AsyncMessageProcessingService
    participant TE as ThreadExecutor
    participant MAC as MessageApiClient
    participant MST as MessageStatusTracker
    
    AC->>MSS: sendBulkMessage(ageGroup, message)
    MSS->>ACS: calculateAgeRange(ageGroup)
    ACS-->>MSS: ageRange(minAge, maxAge)
    
    MSS->>UQS: getUsersByAgeWithPaging(ageRange)
    Note over UQS: 페이지네이션으로 대량 데이터 처리
    
    loop 각 페이지별 처리
        UQS-->>MSS: userBatch
        MSS->>AMPS: processUserBatch(userBatch, message)
        AMPS->>TE: submitAsync(messageSendTask)
        
        par 병렬 메시지 발송
            TE->>MAC: sendMessage(user1, message)
            TE->>MAC: sendMessage(user2, message)
            TE->>MAC: sendMessage(userN, message)
        end
        
        MAC-->>MST: updateSendStatus(results)
    end
    
    MSS-->>AC: bulkSendResponse(statistics)
```

## 연령 계산 시스템

```mermaid
graph TB
    subgraph "Age Calculation Logic"
        SN[Social Number] --> BD[Birth Date]
        BD --> CD[Current Date]
        CD --> AGE[Age Calculation]
        
        AGE --> AG1[10대]
        AGE --> AG2[20대]
        AGE --> AG3[30대]
        AGE --> AG4[40대]
        AGE --> AG5[50대+]
    end
    
    subgraph "Age Group Mapping"
        AG1 --> R1[10-19]
        AG2 --> R2[20-29]
        AG3 --> R3[30-39]
        AG4 --> R4[40-49]
        AG5 --> R5[50+]
    end
    
    style SN fill:#e1f5fe
    style AGE fill:#fff3e0
```

## 페이지네이션 및 배치 처리

```mermaid
graph LR
    subgraph "Database Query Strategy"
        Q[Query] --> P1[Page 1<br/>1000 users]
        Q --> P2[Page 2<br/>1000 users]
        Q --> P3[Page N<br/>remaining]
    end
    
    subgraph "Batch Processing"
        P1 --> B1[Batch 1<br/>100 users x 10]
        P2 --> B2[Batch 2<br/>100 users x 10]
        P3 --> B3[Batch N<br/>variable]
    end
    
    subgraph "Async Execution"
        B1 --> CF1[CompletableFuture]
        B2 --> CF2[CompletableFuture]
        B3 --> CF3[CompletableFuture]
    end
    
    style Q fill:#e8f5e8
    style CF1 fill:#e1f5fe
    style CF2 fill:#e1f5fe
    style CF3 fill:#e1f5fe
```

## 클래스 구조 설계

```mermaid
classDiagram
    class MessageSendDto {
        +String ageGroup
        +String message
        +validate() boolean
    }
    
    class AgeGroup {
        <<enumeration>>
        TEENS
        TWENTIES
        THIRTIES
        FORTIES
        FIFTIES_PLUS
        +getAgeRange() AgeRange
    }
    
    class AgeRange {
        +int minAge
        +int maxAge
        +contains(int age) boolean
    }
    
    class BulkMessageJob {
        +UUID jobId
        +String ageGroup
        +String message
        +LocalDateTime createdAt
        +JobStatus status
        +int totalUsers
        +int successCount
        +int failureCount
    }
    
    class MessageSendService {
        -AgeCalculationService ageCalculationService
        -UserQueryService userQueryService
        -AsyncMessageProcessingService asyncService
        +sendBulkMessage(MessageSendDto) BulkMessageResponse
    }
    
    class AsyncMessageProcessingService {
        -ThreadPoolTaskExecutor executor
        -MessageApiClient messageClient
        +processUserBatch(users, message) CompletableFuture
        +sendMessageAsync(user, message) CompletableFuture
    }
    
    MessageSendDto --> AgeGroup
    AgeGroup --> AgeRange
    MessageSendService --> BulkMessageJob
    AsyncMessageProcessingService --> BulkMessageJob
```

## ThreadPoolTaskExecutor 설정

```mermaid
graph TB
    subgraph "Thread Pool Configuration"
        TC[Task Configuration]
        TC --> CS[Core Size: 10]
        TC --> MS[Max Size: 50]
        TC --> QC[Queue Capacity: 1000]
        TC --> KA[Keep Alive: 60s]
        TC --> TN[Thread Name: message-sender-]
    end
    
    subgraph "Performance Tuning"
        PT[Performance Tuning]
        PT --> RJ[Rejection Policy: CallerRuns]
        PT --> WI[Wait for Tasks: true]
        PT --> TO[Await Termination: 30s]
    end
    
    subgraph "Monitoring Metrics"
        MM[Monitoring Metrics]
        MM --> AT[Active Threads]
        MM --> QS[Queue Size]
        MM --> CT[Completed Tasks]
        MM --> RT[Rejection Count]
    end
```

## 메시지 발송 상태 추적

```mermaid
stateDiagram-v2
    [*] --> CREATED: 작업 생성
    CREATED --> IN_PROGRESS: 처리 시작
    IN_PROGRESS --> PROCESSING_BATCH: 배치 처리 중
    PROCESSING_BATCH --> SENDING_MESSAGES: 메시지 발송 중
    SENDING_MESSAGES --> COMPLETED: 모든 발송 완료
    SENDING_MESSAGES --> PARTIALLY_FAILED: 일부 실패
    PROCESSING_BATCH --> FAILED: 처리 실패
    IN_PROGRESS --> CANCELLED: 작업 취소
    
    COMPLETED --> [*]
    PARTIALLY_FAILED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

## API 설계

### Bulk Message Send Endpoint
```
POST /api/admin/messages/send
Content-Type: application/json
Authorization: Bearer {jwt_token}

Request Body:
{
  "ageGroup": "TWENTIES",
  "message": "할인 쿠폰이 발급되었습니다!"
}

Response:
{
  "success": true,
  "message": "대량 메시지 발송이 시작되었습니다.",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "totalUsers": 15420,
    "estimatedDuration": "PT15M",
    "status": "IN_PROGRESS"
  }
}
```

### Job Status Check Endpoint
```
GET /api/admin/messages/send/{jobId}/status
Authorization: Bearer {jwt_token}

Response:
{
  "success": true,
  "message": "작업 상태 조회 성공",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "totalUsers": 15420,
    "successCount": 15380,
    "failureCount": 40,
    "startedAt": "2024-01-15T10:00:00",
    "completedAt": "2024-01-15T10:12:30",
    "duration": "PT12M30S"
  }
}
```

## 성능 최적화 전략

### 1. 데이터베이스 최적화
```sql
-- 연령별 사용자 조회 최적화 인덱스
CREATE INDEX idx_user_birth_date ON users(social_number);
CREATE INDEX idx_user_status ON users(status) WHERE status = 'ACTIVE';
```

### 2. JVM 튜닝 파라미터
```bash
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
```

### 3. 모니터링 메트릭
- **처리량**: messages/second
- **응답시간**: average, 95th percentile
- **실패율**: failure rate %
- **메모리 사용량**: heap utilization
- **스레드 풀 상태**: active/idle threads

## 에러 처리 및 복구 전략

1. **Database Connection Pool Exhaustion**
   - Connection pool 크기 증가
   - 쿼리 타임아웃 설정

2. **Memory OutOfMemory**
   - 배치 크기 감소
   - GC 튜닝

3. **External API Rate Limiting**
   - 백오프 전략 적용
   - 큐 기반 재시도

4. **Thread Pool Saturation**
   - 동적 스케일링
   - 우선순위 큐 도입

## 보안 고려사항

1. **개인정보 보호**
   - 주민등록번호 마스킹
   - 메시지 내용 로깅 제한

2. **API 접근 제어**
   - 관리자 권한 필수
   - IP 화이트리스트

3. **감사 로깅**
   - 대량 발송 이력 기록
   - 접근 로그 보존