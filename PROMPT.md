# 프롬프트 기록

## 2025-08-04

### 1. Git 환경 초기화
**프롬프트:** "지금부터 이 디렉토리는 자바, 스프링부트 기반의 웹서버 프로젝트 개발로 사용될거야. 깃 환경을 초기화 해줘."

**수행 작업:**
- Git 저장소 초기화 (`git init`)
- Java/Spring Boot 프로젝트용 `.gitignore` 파일 생성

### 2. 프롬프트 기록 파일 생성
**프롬프트:** "PROMPT.md 파일을 별도로 생성하셔서, 앞으로 모든 프롬프트들을 기록해"

**수행 작업:**
- `PROMPT.md` 파일 생성 및 기존 프롬프트 기록

### 3. 초기화 내용 커밋
**프롬프트:** "지금 초기화 내용을 commit해"

**수행 작업:**
- 초기 프로젝트 설정 파일들 커밋

### 4. TaskMaster MCP 프로젝트 커밋
**프롬프트:** "지금 내가 태스크마스터 MCP 프로젝트를 초기화 했으니 git status를 본 뒤에 커밋메세지를 만들어서 커밋해"

**수행 작업:**
- TaskMaster MCP 프로젝트 설정 파일들 추가
- Claude Code 구성 파일 및 MCP 서버 설정 추가
- 환경 변수 예제 파일 생성
- Node.js 및 개발 도구용 gitignore 업데이트

### 5. PRD 분석 및 카테고리화
**프롬프트:** "prd/prd_raw.md 파일을 열고 제품 요구사항을 분석해서 카테고리화해서 정리해줘. 파일명은 prd_calude.md로 저장하고."

**추가 지시사항:**
- "언어는 자바를 사용한다."
- "인증은 JWT로 한다."

**수행 작업:**
- prd/prd_raw.md 파일 분석
- 요구사항을 카테고리별로 정리
- Java와 JWT 인증 기반으로 기술 스펙 명시
- prd/prd_claude.md 파일 생성

### 6. PRD TaskMaster 템플릿 변환
**프롬프트:** "prd/prd_claude.md 파일을 .taskmaster/templates/example_prd.txt 파일을 참조하여, 템플릿의 규격을 맞춘후 prd/prd_task_master.txt로 저장해"

**수행 작업:**
- TaskMaster PRD 템플릿 형식 분석
- prd_claude.md 내용을 <context>와 <PRD> 구조로 재구성
- prd/prd_task_master.txt 파일 생성

### 7. PROMPT.md 갱신 및 커밋 규칙 설정
**프롬프트:** "prompt.md파일을 최신화해. 그리고 커밋해. 그리고 앞으로는 내가 커밋요청을 하면 커밋전 무조건 prompt.md파일을 갱신하도록해."

**수행 작업:**
- PROMPT.md 파일을 최신 상태로 업데이트
- 향후 커밋 시 PROMPT.md 자동 갱신 규칙 설정

### 8. TaskMaster MCP 설정 및 PRD 파싱
**프롬프트:** "prd/prd_task_master.txt 파일로 task_master mcp를 사용할수 있도록 해"

**수행 작업:**
- TaskMaster PRD 파싱으로 10개 태스크 생성
- 단계별 의존성 관계 설정 (1→2→3→...→10)
- 각 태스크의 우선순위 및 복잡도 설정

### 9. TaskMaster 복잡도 분석 요청
**프롬프트:** "태스크마스터의 복잡도를 분석해줘. analyze-complexity명령어로 알고있다."

**수행 작업:**
- TaskMaster analyze-complexity 명령어 실행
- 10개 태스크의 복잡도 분석 완료 (높은 복잡도 4개, 중간 복잡도 6개)

### 10. H2 메모리 DB 반영 요청
**프롬프트:** "내가 이미 tm expand --all로 모든 태스크를 서브태스크로 분해했다. tm list --with-subtasks로 잘 분해된 것을 확인 하였다. DB는 H2 메모리 DB를 사용하도록 반영해라."

**수행 작업:**
- TaskMaster update 명령으로 모든 태스크에 H2 메모리 DB 설정 반영
- H2 인메모리 데이터베이스를 기본 설정으로 변경
- MySQL/PostgreSQL은 선택적 대안으로 유지

### 11. PROMPT.md 갱신 및 커밋 요청
**프롬프트:** "프롬프트를 갱신하고 커밋하라"

**수행 작업:**
- PROMPT.md 파일 최신 내용으로 갱신
- 변경사항 커밋 진행

### 12. 개발 표준 학습 및 1번 태스크 시작
**프롬프트:** "이제 스프링 부트 프로젝트를 본격 개발하기에 앞서, rule/rule.txt의 내용을 학습해둬." 
"그럼 이제 태스크마스터의 1번 작업을 시작해. DB는 H2 메모리 DB로 해야 한다. 다른 서버 컴퓨터에서 바로 실행해도 문제 되지 않도록."

**수행 작업:**
- Spring Boot 개발 표준 컨텍스트 학습 완료
- TaskMaster 1번 태스크 완료: Spring Boot 2.7.18 + Java 11 프로젝트 초기 설정
- H2 메모리 DB 설정, Gradle 빌드 시스템, 기본 패키지 구조 생성
- 애플리케이션 빌드 및 실행 성공 확인

### 13. Spring Boot 및 JDK 버전 업그레이드 요청
**프롬프트:** "스프링부트 3.5.4 버전과 jdk는 21버전으로 바꿔줘."

**수행 작업:**
- Spring Boot 버전을 3.5.4로 업그레이드
- JDK 버전을 21로 변경