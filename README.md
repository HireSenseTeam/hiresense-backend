# Hiresense Backend 🌟

> AI 기반 맞춤형 면접 플랫폼 Hiresense의 백엔드 서버

## 📖 프로젝트 소개

Hiresense는 **이력서·채용공고를 분석해 AI가 맞춤형 면접 질문을 만들고**, 답변을 **비동기로 채점**해 **공정한 랭킹**으로 순위를 보여주는 **AI 면접 플랫폼**입니다.  
지원자는 맞춤 질문에 답하고, 채용 담당자는 자동 채점·랭킹으로 효율적인 선발이 가능합니다.

## ✨ 핵심 기능

### 🔐 **1. 로그인/회원가입**
- **이메일 기반**: 회원가입·로그인
- **JWT 기반**: Access Token + Refresh Token
- **Redis**: Refresh Token 저장, 로그아웃 시 Access Token Blacklist (TTL 기반)
- **Refresh Token Rotation**: 재발급 시 기존 토큰 삭제로 탈취 피해 최소화

### 📝 **2. 이력서·채용공고 관리**
- **이력서 CRUD**: 학력, 희망 직무, 경력 수준, 근무 조건 등
- **채용공고 CRUD**: 회사·직무·자격요건·인재상 등
- **역할 분리**: 지원자(APPLICANT) / 채용 담당자(COMPANY)

### 🤖 **3. AI 맞춤형 면접 질문**
- **이력서 기반 질문**: AWS Bedrock(Claude)으로 이력서 분석 후 질문 생성
- **채용공고 기반 질문**: 공고 내용에 맞는 질문 생성
- **꼬리 질문**: 답변 흐름에 따른 실시간 후속 질문 생성
- **비동기 생성**: 이력서/공고 저장 시 백그라운드에서 질문 생성

### 🎤 **4. 면접 세션·답변**
- **면접 시작**: 공통 + 이력서 + 공고 질문 조합, 세션(UUID) 생성
- **답변 제출**: 질문 순서대로 답변 저장, 세션 상태 관리(IN_PROGRESS → COMPLETED)
- **재접속**: 진행 중 세션 조회로 이어하기 지원

### ⚡ **5. 비동기 AI 채점**
- **즉시 응답**: 면접 종료 API는 답변·세션 처리 후 바로 반환 (채점은 백그라운드)
- **AWS Bedrock**: 정량 점수(총점, 인재상 적합도, 업무 적합도) + 정성 피드백(강점/약점/코멘트)
- **트랜잭션·영속성**: ID 전달 + 재조회 패턴으로 LazyInitializationException 방지

### 🏆 **6. 랭킹 시스템**
- **Standard Competition Ranking**: 동점자 동일 순위, 공정한 순위 표시
- **반정규화**: InterviewScore에 jobPostingId, applicantEmail, applicantName 저장 → 조인 없이 단일 테이블 조회
- **인덱스 기반 정렬**: `findByJobPostingIdOrderByOverallScoreDesc`로 성능 확보

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 17
- **Security**: Spring Security + JWT
- **Database**: MySQL 8 (RDS)
- **Cache / Token**: Redis (Refresh Token, Access Token Blacklist)
- **ORM**: Spring Data JPA

### AI
- **AWS Bedrock**: Claude 3 Haiku (질문 생성·채점)

### DevOps
- **Build Tool**: Gradle
- **Deployment**: AWS EC2 (JAR 배포, deploy.sh)
- **API 문서**: SpringDoc OpenAPI (Swagger UI)

### 인증·보안
- **JWT**: Access Token(1시간), Refresh Token(7일)
- **Redis**: Refresh Token 저장(7일 TTL), Blacklist(동적 TTL)

## 🚀 빠른 시작

### 로컬 개발 환경 설정

#### 1. 저장소 클론
```bash
git clone <repository-url>
cd hiresense-backend
```

#### 2. 환경변수 설정
```bash
# application.yml / application-dev.yml 참고
# JWT_SECRET, DB URL, Redis, AWS Bedrock 등 설정
```

#### 3. DB·Redis 실행
```bash
# MySQL, Redis는 로컬 또는 Docker로 실행
# 예: docker run -p 3306:3306 mysql:8
#     docker run -p 6379:6379 redis:alpine
```

#### 4. 애플리케이션 실행
```bash
./gradlew bootRun
# 또는 IDE에서 HiresenseBackendApplication 실행
```

## 🏗 프로젝트 구조

```
src/
├── main/
│   ├── java/com/hiresense/
│   │   ├── auth/           # 인증/인가 (JWT, Refresh, Blacklist)
│   │   ├── ai/             # AWS Bedrock (질문 생성, 채점)
│   │   ├── user/           # 사용자·역할
│   │   ├── resume/         # 이력서 CRUD
│   │   ├── jobPosting/     # 채용공고 CRUD
│   │   ├── question/       # 질문 조회 (공통/이력서/공고)
│   │   ├── interview/      # 면접 세션·답변·채점·랭킹
│   │   ├── global/         # 예외 처리, 설정(Async 등), 공통
│   │   └── config/         # Security, Redis, Swagger, JWT Filter
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
└── test/                   # 테스트 코드
```

## 🎯 개발 로드맵

### ✅ **Phase 1 - 서버리스 MVP (완료)**
- Python Flask + AWS Lambda + DynamoDB
- AI 면접 질문 생성 PoC

### ✅ **Phase 2 - Spring 기반 고도화 (현재)**
- Java 17 + Spring Boot + MySQL(RDS)
- 트랜잭션 기반 데이터 정합성
- 비동기 AI 채점, N+1 최적화(EntityGraph), 랭킹 반정규화
- JWT Blacklist + Refresh Token Rotation
- **AI-DEAS 학습동아리 대상 수상**

### 🔄 **Phase 3 - 확장 (예정)**
- 채점 실패 재시도 (Spring Retry / 메시지 큐)
- CI/CD (GitHub Actions, ECR 등)
- 모니터링·메트릭

