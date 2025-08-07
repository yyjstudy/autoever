# AutoEver 회원관리 시스템

> **사용한 AI 모델**: Claude Sonnet 4  
> **전략**: AI 주도개발 (바이브코딩)  
> **MCP**: 태스크마스터  
> **프롬프트 확인**: PROMPT.md

Spring Boot 기반의 회원관리 및 메시지 발송 시스템입니다. JWT 인증과 H2 인메모리 데이터베이스를 사용합니다.

## ✨ 주요 기능

### 👤 회원관리 시스템
- **회원가입/로그인**: JWT 기반 인증 시스템
- **회원 정보 관리**: CRUD 기능 및 연령대별 분류
- **관리자 기능**: 회원 조회, 수정, 삭제 권한

### 📱 메시지 발송 시스템
- **큐 기반 처리**: 최대 1,500개 메시지 대기열로 안정적 처리
- **Fallback 메커니즘**: KakaoTalk → SMS 자동 전환
- **Rate Limiting**: KakaoTalk 100건/분, SMS 500건/분 제한
- **실시간 통계**: 채널별 성공/실패 건수 실시간 추적
- **연령대별 대량 발송**: 10대~50대 이상 연령대별 일괄 발송

## 기술 스택

- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **H2 Database** (인메모리)
- **Gradle 8.10**
- **Mock Servers** (KakaoTalk, SMS API 시뮬레이션)

## 로컬 실행 방법

### 사전 요구사항
- Java 21 이상 설치
- Git 설치

### 실행 단계

1. **저장소 클론**
   ```bash
   git clone <repository-url>
   cd autoever1
   ```

2. **전체 시스템 실행** (권장)
   ```bash
   ./start.sh
   ```
   > 🚀 **한 번에 모든 서버 시작**: Mock 서버들과 메인 애플리케이션을 자동으로 시작합니다.

3. **개별 서버 실행** (개발용)
   ```bash
   # KakaoTalk Mock Server (별도 터미널)
   ./gradlew :mock-servers:kakaotalk-mock:bootRun
   
   # SMS Mock Server (별도 터미널)
   ./gradlew :mock-servers:sms-mock:bootRun
   
   # 메인 애플리케이션 (별도 터미널)
   ./gradlew bootRun
   ```
   
   또는 JAR 파일 빌드 후 실행:
   ```bash
   ./gradlew build
   java -jar build/libs/autoever-member-system-0.0.1-SNAPSHOT.jar
   ```

4. **애플리케이션 접속**
   - **API 서버**: http://localhost:8080
   - **Swagger UI**: http://localhost:8080/swagger-ui/index.html (admin/1212)
   - **KakaoTalk Mock**: http://localhost:8081
   - **SMS Mock**: http://localhost:8082



## API 엔드포인트

### 🔐 인증 API
```
POST /api/users/register  # 회원가입
POST /api/users/login     # 로그인
POST /api/users/logout    # 로그아웃
GET  /api/users/me        # 본인 정보 조회
```

### 👨‍💼 관리자 API
```
GET    /api/admin/users              # 회원 목록 조회 (페이징)
GET    /api/admin/users/{id}         # 특정 회원 조회
PUT    /api/admin/users/{id}         # 회원 정보 수정
DELETE /api/admin/users/{id}         # 회원 삭제
```

### 📨 메시지 발송 API
```
POST /api/admin/messages/send         # 대량 메시지 발송
GET  /api/admin/messages/statistics   # 발송 통계 조회
POST /api/admin/messages/statistics/reset # 통계 초기화
```

**관리자 인증**: Basic Auth (admin / 1212)

## 📊 메시지 발송 통계

실시간으로 다음 통계를 제공합니다:
- **totalAttempts**: 전체 발송 시도 수
- **kakaoSuccessCount**: 카카오톡 성공 건수
- **smsSuccessCount**: SMS 성공 건수
- **failureCount**: 실패 건수
- **currentQueueSize**: 현재 큐 대기 건수
- **maxQueueSize**: 최대 큐 용량 (1,500개)

## 🏗️ 시스템 아키텍처

전체 시스템 아키텍처와 메시지 플로우는 [design/messaging/current-architecture.md](design/messaging/current-architecture.md)에서 확인할 수 있습니다.

### 핵심 컴포넌트
- **MessageQueueService**: 메시지 대기열 관리
- **MessageQueueProcessor**: 백그라운드 메시지 처리 (10ms 주기)
- **ApiRateLimiter**: 슬라이딩 윈도우 기반 속도 제한
- **MessageSendTracker**: 실시간 통계 수집
- **FallbackMessageService**: KakaoTalk → SMS 자동 전환

## 데이터베이스

- **H2 인메모리 데이터베이스** 사용
- 애플리케이션 재시작 시 데이터 초기화
- 별도 데이터베이스 설치 불필요

## 개발 환경

### 빌드
```bash
./gradlew build
```

### 테스트 (358개 테스트)
```bash
./gradlew test
```

### 개발 서버 실행 (DevTools)
```bash
./gradlew bootRun
```

## 외부 의존성

이 프로젝트는 Mock 서버와 연동됩니다:

- **KakaoTalk Mock API**: http://localhost:8081/kakaotalk-messages
- **SMS Mock API**: http://localhost:8082/sms

실제 운영에서는 실제 카카오톡/SMS API로 교체 가능합니다.

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/autoever/member/
│   │   ├── controller/           # REST 컨트롤러
│   │   ├── service/              # 비즈니스 로직
│   │   ├── repository/           # 데이터 액세스
│   │   ├── entity/               # JPA 엔티티
│   │   ├── dto/                  # 데이터 전송 객체
│   │   ├── config/               # 설정 클래스
│   │   ├── exception/            # 예외 처리
│   │   └── message/              # 메시지 발송 시스템
│   │       ├── client/           # 외부 API 클라이언트
│   │       ├── queue/            # 메시지 큐 시스템
│   │       ├── ratelimit/        # Rate Limiting
│   │       ├── result/           # 통계 및 결과 추적
│   │       ├── service/          # 메시지 서비스
│   │       └── template/         # 메시지 템플릿
│   └── resources/
│       └── application.yml       # 애플리케이션 설정
├── test/                         # 테스트 코드 (358개)
└── mock-servers/                 # Mock 서버들
    ├── kakaotalk-mock/           # KakaoTalk API Mock
    └── sms-mock/                 # SMS API Mock
```

## 보안

- **JWT 토큰** 기반 인증
- **BCrypt** 패스워드 해싱
- **Basic Auth** (관리자 API)
- **Spring Security** 설정
- **CORS** 설정
- **CSRF** 보호

## 📈 성능 특징

- **큐 기반 처리**: 대량 메시지 처리 시 안정성 확보
- **Rate Limiting**: API 호출 제한으로 외부 서비스 보호
- **비동기 처리**: 대량 발송 요청 즉시 응답
- **Fallback 메커니즘**: 높은 가용성 보장
- **실시간 통계**: 발송 현황 실시간 모니터링

## 🧪 테스트 커버리지

- **총 358개** 테스트 케이스
- 단위 테스트, 통합 테스트 포함
- Mock 서버 연동 테스트
- API 엔드포인트 테스트
- 메시지 발송 플로우 테스트

## 📝 Swagger를 활용한 메시지 전송 테스트

### 1단계: 테스트 사용자 생성
Swagger UI에서 **`/api/test/users`** 엔드포인트를 사용하여 테스트 사용자를 생성합니다.

### 2단계: 메시지 전송
Swagger UI에서 **`/api/admin/messages/send`** 엔드포인트를 사용하여 대량 메시지를 발송합니다.

### 3단계: 결과 확인
Swagger UI에서 **`/api/admin/messages/statistics`** 엔드포인트를 사용하여 발송 통계를 실시간으로 확인할 수 있습니다.

## 🤖 Task Master (개발 관리)

이 프로젝트는 Task Master MCP를 사용하여 개발 작업을 체계적으로 관리합니다.

**작업 현황 확인**: `.taskmaster/tasks/tasks.json` 파일에서 모든 작업 목록을 직접 확인할 수 있습니다.

### 주요 명령어

#### 작업 목록 확인
```bash
task-master list                    # 모든 작업 목록 보기
task-master next                    # 다음 해야 할 작업 보기
```

#### 작업 상세 정보
```bash
task-master show 1                  # 작업 1번의 상세 정보
task-master show 2.3                # 작업 2.3번의 상세 정보
```

#### 작업 상태 관리
```bash
task-master set-status --id=1.2 --status=done    # 작업 완료 표시
task-master set-status --id=2 --status=in-progress # 작업 진행 중 표시
```

#### 복잡도 분석
```bash
task-master analyze-complexity --research         # 작업 복잡도 분석
task-master complexity-report                     # 복잡도 보고서 확인
```

