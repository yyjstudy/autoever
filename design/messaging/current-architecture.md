# 현재 메시지 전송 시스템 아키텍처

## 전체 아키텍처

```mermaid
graph TB
    subgraph "Admin Interface"
        UI[관리자 웹 UI]
        API[AdminMessageController]
    end
    
    subgraph "Message Processing Layer"
        BMS[BulkMessageService]
        FMS[FallbackMessageService]
        MTS[MessageTemplateService]
        UQS[UserQueryService]
        BPS[BatchProcessingService]
    end
    
    subgraph "Queue & Processing"
        MQS[MessageQueueService]
        MQP[MessageQueueProcessor]
        QUEUE[(Message Queue<br/>Max: 1500)]
    end
    
    subgraph "Rate Limiting & Tracking"
        ARL[ApiRateLimiter]
        MST[MessageSendTracker]
    end
    
    subgraph "External APIs"
        KAPI[KakaoTalk API Client]
        SAPI[SMS API Client]
        KMOCK[KakaoTalk Mock Server]
        SMOCK[SMS Mock Server]
    end
    
    subgraph "Database"
        DB[(PostgreSQL)]
    end
    
    %% Main Flow
    UI --> API
    API --> BMS
    BMS --> UQS
    UQS --> DB
    BMS --> BPS
    BPS --> FMS
    FMS --> MTS
    FMS --> MQS
    MQS --> QUEUE
    
    %% Background Processing
    MQP --> QUEUE
    MQP --> ARL
    MQP --> KAPI
    MQP --> SAPI
    MQP --> MST
    
    %% External Connections
    KAPI --> KMOCK
    SAPI --> SMOCK
    
    %% Statistics
    API --> MST
    
    style QUEUE fill:#e1f5fe
    style MST fill:#f3e5f5
    style ARL fill:#fff3e0
```

## 메시지 전송 시퀀스 다이어그램

### 1. 대량 메시지 발송 요청
```mermaid
sequenceDiagram
    participant 관리자
    participant API
    participant BulkService
    participant Queue
    
    관리자->>API: POST /messages/send<br/>{"ageGroup": "TWENTIES", "message": "할인쿠폰!"}
    API->>BulkService: 대량 발송 시작
    
    alt 큐가 가득참
        BulkService-->>API: 503 Service Unavailable
        API-->>관리자: "큐가 가득참니다"
    else 정상 처리
        BulkService->>Queue: 15,000명 메시지를 큐에 추가
        BulkService-->>API: 202 Accepted
        API-->>관리자: "발송이 시작되었습니다"
    end
```

### 2. 백그라운드 메시지 처리 (10ms마다 실행)
```mermaid
sequenceDiagram
    participant Processor
    participant Queue
    participant RateLimit
    participant KakaoAPI
    participant SMS_API
    participant Tracker
    
    Processor->>Queue: 메시지 1개 꺼내기
    Processor->>RateLimit: 카카오톡 발송 가능?
    
    alt 카카오톡 성공
        RateLimit-->>Processor: OK
        Processor->>KakaoAPI: 메시지 발송
        KakaoAPI-->>Processor: 성공
        Processor->>Tracker: 카카오톡 성공 기록
    else 카카오톡 실패 → SMS 대체
        KakaoAPI-->>Processor: 실패
        Processor->>SMS_API: SMS 발송
        SMS_API-->>Processor: 성공
        Processor->>Tracker: SMS 성공 기록
    else 모든 채널 실패
        SMS_API-->>Processor: 실패
        Processor->>Tracker: 실패 기록
    end
```

### 3. 통계 조회
```mermaid
sequenceDiagram
    participant 관리자
    participant API
    participant Tracker
    
    관리자->>API: GET /messages/statistics
    API->>Tracker: 통계 요청
    Tracker-->>API: {"totalAttempts": 1523,<br/>"kakaoSuccessCount": 1089,<br/>"smsSuccessCount": 352,<br/>"failureCount": 82}
    API-->>관리자: 통계 데이터 반환
```

## 주요 구성 요소 설명

### 1. **메시지 큐 시스템**
- **용량**: 최대 1500개 메시지
- **처리 주기**: 10ms마다 백그라운드 처리
- **큐 가득참 시**: 즉시 실패 응답 (503 Service Unavailable)

### 2. **Rate Limiting**
- **KakaoTalk**: 분당 100건 제한
- **SMS**: 분당 500건 제한
- **슬라이딩 윈도우** 방식으로 정확한 속도 제어

### 3. **Fallback 메커니즘**
- **1순위**: KakaoTalk 발송 시도
- **2순위**: 실패 시 SMS로 자동 전환
- **최종 실패**: 모든 채널 실패 시 FAILED_BOTH 기록

### 4. **통계 수집**
- **실시간 추적**: 모든 발송 결과 실시간 카운팅
- **채널별 분리**: 카카오톡/SMS 성공 건수 별도 집계
- **정확성 보장**: totalAttempts = kakaoSuccessCount + smsSuccessCount + failureCount

### 5. **템플릿 시스템**
- **자동 추가**: "#{회원명}님, 안녕하세요. 현대 오토에버입니다. #{원본메시지}"
- **개인화**: 각 회원별 이름 자동 치환

### 6. **배치 처리**
- **배치 크기**: 100명씩 처리
- **비동기 실행**: 대량 발송 시 즉시 응답 후 백그라운드 처리
- **진행상황 추적**: 실시간 성공/실패 카운팅

### 7. **에러 처리**
- **큐 포화**: 즉시 실패 응답으로 사용자 경험 개선  
- **Rate Limit**: 자동 대기 후 재시도
- **API 장애**: 채널 간 Fallback으로 가용성 확보