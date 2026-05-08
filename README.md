# Hiresense Backend

AWS Bedrock 기반 맞춤형 AI 면접 및 자동 채점 채용 플랫폼의 Spring Boot 백엔드입니다.

서버리스 MVP에서 출발한 프로젝트를 Spring Boot + MySQL 구조로 재설계하며, 면접 세션의 데이터 정합성, AI 채점 지연 시간, 랭킹 조회 성능, JWT 보안 한계를 개선하는 데 집중했습니다.

## Portfolio Summary

### 1. Serverless MVP 한계 개선

**문제**
- DynamoDB 기반 MVP에서는 이력서, 질문, 답변, 점수처럼 여러 데이터가 함께 변경되는 흐름에서 트랜잭션 관리가 어려웠습니다.
- 랭킹, 복합 필터링처럼 정렬과 조건 조회가 필요한 기능에서 Scan 기반 처리가 늘어났습니다.

**해결**
- Spring Boot + MySQL(RDS) 기반으로 전환하고, 면접 세션, 답변, 점수의 관계를 RDB 모델로 재설계했습니다.
- `@Transactional`을 적용해 세션 단위 저장 흐름의 원자성을 확보했습니다.

**검증**
- 50명 채점 결과 저장 테스트에서 데이터 유실과 중복이 발생하지 않음을 검증했습니다.
- 동일 세션 중복 채점은 DB Unique 제약으로 차단됩니다.

### 2. 비동기 AI 채점 파이프라인

**문제**
- AWS Bedrock 기반 AI 채점은 수 초의 지연 시간이 발생해, 동기 처리 시 사용자가 면접 종료 응답을 오래 기다려야 했습니다.
- `@Async` 적용 시 기존 영속성 컨텍스트가 다른 스레드로 전파되지 않아 LazyInitializationException 위험이 있었습니다.

**해결**
- 면접 종료 API는 답변 저장과 세션 상태 변경 후 즉시 응답합니다.
- 비동기 채점 메서드에는 엔티티가 아니라 `sessionId`, `jobPostingId`, `applicantEmail`만 전달합니다.
- 비동기 스레드 내부에서 새 트랜잭션으로 필요한 엔티티를 재조회합니다.
- 답변 저장 트랜잭션이 커밋된 뒤 채점이 시작되도록 `afterCommit` 시점에 비동기 작업을 등록했습니다.
- 면접 상태를 `IN_PROGRESS -> COMPLETED -> SCORING -> SCORED/SCORING_FAILED`로 분리했습니다.

**검증**
- 시뮬레이션 테스트 기준, 동기 채점 응답 약 6초를 비동기 응답 수 ms 수준으로 단축했습니다.
- 채점 실패 시 세션 상태를 `SCORING_FAILED`로 남겨 클라이언트가 조회 가능한 상태를 유지합니다.

### 3. N+1 및 랭킹 조회 최적화

**문제**
- 면접 세션 목록 조회 시 `JobPosting`, `Resume` 연관관계로 인해 N+1 쿼리 문제가 발생할 수 있었습니다.
- 랭킹 조회에서 매번 여러 테이블을 조인하면 데이터 증가 시 병목이 커질 수 있었습니다.

**해결**
- `@EntityGraph`로 세션 목록 조회 시 필요한 연관 엔티티를 한 번에 로딩합니다.
- `InterviewScore`에 랭킹 조회용 필드인 `jobPostingId`, `applicantEmail`, `applicantName`을 반정규화했습니다.
- `job_posting_id, overall_score` 복합 인덱스를 추가해 공고별 점수 정렬 조회에 맞췄습니다.
- 세션 목록의 질문 개수 조회는 세션별 count 반복 대신 group by 집계 쿼리로 처리합니다.

**검증**
- 세션 조회 쿼리 수를 22회에서 1회로 줄이는 시나리오를 테스트로 검증했습니다.
- 랭킹 조회는 조인 기반 조회 대신 단일 테이블 조회 구조로 단순화했습니다.

### 4. JWT + Redis 인증 보안

**문제**
- JWT는 Stateless 특성상 로그아웃 이후에도 토큰 만료 전까지 재사용될 수 있습니다.
- Refresh Token 탈취 시 Access Token을 반복 발급받을 위험이 있습니다.

**해결**
- 로그아웃 시 Access Token을 Redis blacklist에 저장하고, 인증 필터에서 재사용을 차단합니다.
- blacklist TTL은 Access Token의 남은 만료 시간으로 설정해 자동 정리됩니다.
- Refresh Token Rotation을 적용해 재발급 시 기존 Refresh Token을 폐기합니다.

**검증**
- 로그아웃 후 Access Token 재사용 차단, Refresh Token Rotation, Redis TTL 자동 정리 시나리오를 테스트로 검증했습니다.

## Main Features

- 이메일 기반 회원가입 및 로그인
- JWT Access Token + Refresh Token 발급
- Redis 기반 Refresh Token 저장 및 Access Token blacklist
- 지원자 이력서 CRUD
- 기업 채용공고 CRUD
- AWS Bedrock Claude 기반 이력서/채용공고 맞춤 질문 생성
- 면접 세션 시작, 답변 제출, 이어하기 조회
- 비동기 AI 자동 채점
- 점수 및 정성 피드백 조회
- 공고별 Standard Competition Ranking 조회

## Tech Stack

- Java 17
- Spring Boot 3.5.6
- Spring Security
- Spring Data JPA
- MySQL 8
- Redis
- AWS Bedrock Claude 3 Haiku
- SpringDoc OpenAPI
- Gradle

## Architecture

```text
Client
  -> Spring Security JWT Filter
  -> Controller
  -> Service
  -> Repository
  -> MySQL

Auth Service
  -> Redis Refresh Token
  -> Redis Access Token Blacklist

Interview Service
  -> Save answer and set session status to SCORING
  -> @Async InterviewScoringService
  -> AWS Bedrock
  -> Save InterviewScore
  -> Set session status to SCORED or SCORING_FAILED
```

## Domain Flow

```text
IN_PROGRESS
  -> answer submitted
  -> COMPLETED
  -> SCORING
  -> SCORED

SCORING
  -> Bedrock/API/parsing error
  -> SCORING_FAILED
```

## Local Run

### 1. Requirements

- Java 17
- MySQL 8
- Redis

### 2. Environment Variables

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_PASSWORD=your-local-db-password
export JWT_SECRET=your-256-bit-secret
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

AWS Bedrock을 실제로 사용할 경우 AWS credential과 Bedrock 권한이 필요합니다.

### 3. Run

```bash
./gradlew bootRun
```

Swagger UI:

```text
http://localhost:8000/swagger-ui/index.html
```

## Test

테스트는 `test` 프로필과 H2 기반으로 독립 실행됩니다.

```bash
./gradlew test
```

주요 테스트 범위:

- 비동기 채점 응답 시간 비교
- 면접 상태 전이
- N+1 쿼리 최적화
- 랭킹 조회 성능 비교
- 트랜잭션 및 Unique 제약 기반 데이터 정합성
- JWT blacklist, Refresh Token Rotation, Redis TTL

## Production Profile

`prod` 프로필은 환경변수 기반 설정을 사용합니다.

필수 환경변수:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://...
export DB_USERNAME=...
export DB_PASSWORD=...
export REDIS_HOST=...
export REDIS_PORT=6379
export JWT_SECRET=...
export CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

운영 프로필에서는 `ddl-auto=validate`를 사용해 스키마 자동 변경을 막습니다.
