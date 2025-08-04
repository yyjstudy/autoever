# AutoEver 회원관리 시스템

Spring Boot 기반의 회원관리 시스템입니다. JWT 인증과 H2 인메모리 데이터베이스를 사용합니다.

## 기술 스택

- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **H2 Database** (인메모리)
- **Gradle 8.10**

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

2. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```
   
   또는 JAR 파일 빌드 후 실행:
   ```bash
   ./gradlew build
   java -jar build/libs/autoever-member-system-0.0.1-SNAPSHOT.jar
   ```

3. **애플리케이션 접속**
   - API 서버: http://localhost:8080
   - H2 콘솔: http://localhost:8080/h2-console
     - JDBC URL: `jdbc:h2:mem:memberdb`
     - Username: `sa`
     - Password: (빈 값)

## API 엔드포인트

### 회원가입
```
POST /api/users/register
```

### 로그인
```
POST /api/users/login
```

### 본인 정보 조회
```
GET /api/users/me
Authorization: Bearer <JWT_TOKEN>
```

### 관리자 API
```
GET /api/admin/users          # 회원 조회 (페이징)
PUT /api/admin/users/{id}     # 회원 정보 수정
DELETE /api/admin/users/{id}  # 회원 삭제
POST /api/admin/messages/send # 메시지 발송
```

**관리자 인증**: Basic Auth (admin / 1212)

## 데이터베이스

- **H2 인메모리 데이터베이스** 사용
- 애플리케이션 재시작 시 데이터 초기화
- 별도 데이터베이스 설치 불필요

## 개발 환경

### 빌드
```bash
./gradlew build
```

### 테스트
```bash
./gradlew test
```

### 개발 서버 실행 (DevTools)
```bash
./gradlew bootRun
```

## 외부 의존성

이 프로젝트는 외부 메시지 API와 연동됩니다:

- **카카오톡 API**: http://localhost:8081/kakaotalk-messages
- **SMS API**: http://localhost:8082/sms

테스트 시에는 해당 서버들이 실행 중이어야 합니다.

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/autoever/member/
│   │   ├── controller/     # REST 컨트롤러
│   │   ├── service/        # 비즈니스 로직
│   │   ├── repository/     # 데이터 액세스
│   │   ├── entity/         # JPA 엔티티
│   │   ├── dto/           # 데이터 전송 객체
│   │   ├── config/        # 설정 클래스
│   │   └── exception/     # 예외 처리
│   └── resources/
│       └── application.yml # 애플리케이션 설정
└── test/                  # 테스트 코드
```

## 보안

- JWT 토큰 기반 인증
- BCrypt 패스워드 해싱
- Basic Auth (관리자)
- Spring Security 설정