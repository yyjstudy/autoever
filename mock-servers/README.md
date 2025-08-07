# Mock Servers

메인 애플리케이션의 외부 API 의존성을 시뮬레이션하기 위한 Mock 서버들입니다.

## 서버 구성

### KakaoTalk Mock Server (포트: 8081)
- **엔드포인트**: `POST /kakaotalk-messages`
- **인증**: Basic Auth (autoever/1234)
- **요청 형식**: JSON
- **응답 형식**: JSON

### SMS Mock Server (포트: 8082)
- **엔드포인트**: `POST /sms`
- **인증**: Basic Auth (autoever/5678)
- **요청 형식**: application/x-www-form-urlencoded
- **응답 형식**: JSON

## 빠른 시작

### 1. 서버 시작
```bash
# 두 서버 모두 시작
./scripts/start-mock-servers.sh

# 또는 개별적으로 시작
./gradlew :mock-servers:kakaotalk-mock:bootRun    # 포트 8081
./gradlew :mock-servers:sms-mock:bootRun          # 포트 8082
```

### 2. API 테스트
```bash
# 자동화된 테스트 실행
./scripts/test-mock-apis.sh

# 또는 수동 테스트
curl -X POST -u autoever:1234 -H 'Content-Type: application/json' \
     -d '{"phone":"010-1234-5678","message":"테스트 메시지"}' \
     http://localhost:8081/kakaotalk-messages

curl -X POST -u autoever:5678 -H 'Content-Type: application/json' \
     -d '{"message":"테스트메시지"}' \
     'http://localhost:8082/sms?phone=010-1234-5678'
```

### 3. 서버 종료
```bash
./scripts/stop-mock-servers.sh
```

## API 명세

### KakaoTalk Mock API

#### 요청 예시
```json
POST /kakaotalk-messages
Authorization: Basic YXV0b2V2ZXI6MTIzNA==
Content-Type: application/json

{
  "phone": "010-1234-5678",
  "message": "안녕하세요. 현대 오토에버입니다."
}
```

#### 성공 응답 (200 OK)
```
응답 바디 없음
```

#### 실패 응답 (4xx/5xx)
```
응답 바디 없음
```

### SMS Mock API

#### 요청 예시
```json
POST /sms?phone=010-1234-5678
Authorization: Basic YXV0b2V2ZXI6NTY3OA==
Content-Type: application/json

{
  "message": "SMS 테스트 메시지입니다"
}
```

#### 성공 응답 (200 OK)
```json
{
  "result": "OK",
  "messageId": "sms_x1y2z3w4",
  "timestamp": 1692345678901
}
```

#### 실패 응답 (4xx/5xx)
```json
{
  "result": "ERROR",
  "errorCode": "QUOTA_EXCEEDED",
  "error": "일일 발송량을 초과했습니다",
  "timestamp": 1692345678901
}
```

## 에러 시나리오 시뮬레이션

특정 전화번호 패턴으로 다양한 에러 상황을 테스트할 수 있습니다:

### KakaoTalk Mock
- `xxx-9999-xxxx`: 500 - 서버 오류
- `xxx-8888-xxxx`: 503 - 네트워크 오류  
- `xxx-7777-xxxx`: 408 - 타임아웃

### SMS Mock
- `xxx-9999-xxxx`: 429 - 발송량 초과
- `xxx-8888-xxxx`: 500 - 서버 오류
- `xxx-7777-xxxx`: 503 - 네트워크 오류
- `xxx-6666-xxxx`: 400 - 잘못된 수신번호

## 로그 확인

```bash
# 실시간 로그 확인
tail -f logs/kakaotalk-mock.log
tail -f logs/sms-mock.log

# 또는 두 로그 동시에 확인
tail -f logs/kakaotalk-mock.log logs/sms-mock.log
```

## IntelliJ에서 실행

### 방법 1: Main 클래스 실행
1. `KakaoTalkMockApplication.java` 또는 `SmsMockApplication.java` 열기
2. 클래스 옆의 녹색 재생 버튼 클릭

### 방법 2: Gradle Tasks
1. IntelliJ 오른쪽 Gradle 패널 열기
2. `Tasks > application > bootRun` 더블클릭

### 방법 3: Run Configuration
1. Run/Debug Configurations 열기
2. Spring Boot Configuration 추가
3. Main Class와 Module 설정

## 개발 시 주의사항

1. **포트 충돌**: 8081, 8082 포트가 이미 사용 중인지 확인
2. **인증 정보**: KakaoTalk(autoever/1234), SMS(autoever/5678)
3. **동시 실행**: 두 서버는 독립적으로 동시 실행 가능
4. **로그 레벨**: application.yml에서 로그 레벨 조정 가능

## 프로젝트 구조

```
mock-servers/
├── README.md                      # 이 파일
├── kakaotalk-mock/               # KakaoTalk Mock Server
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../
│       │   ├── KakaoTalkMockApplication.java
│       │   ├── config/SecurityConfig.java
│       │   ├── controller/KakaoTalkController.java
│       │   └── dto/
│       └── resources/application.yml
└── sms-mock/                     # SMS Mock Server
    ├── build.gradle
    └── src/main/
        ├── java/.../
        │   ├── SmsMockApplication.java
        │   ├── config/SecurityConfig.java
        │   ├── controller/SmsController.java
        │   └── dto/
        └── resources/application.yml
```