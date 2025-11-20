# GitHub Issues 템플릿

## 이슈 작성 가이드

각 기능별로 아래 템플릿을 사용하여 이슈를 생성하세요.

---

## Issue #1: 프로젝트 초기 설정 및 공통 유틸리티

### 📋 설명
Spring Boot 프로젝트의 기본 설정과 공통으로 사용되는 유틸리티 클래스들을 구현합니다.

### ✅ 작업 내용
- [ ] Spring Boot 프로젝트 초기 설정
- [ ] build.gradle 의존성 설정 (Spring Boot, JPA, MySQL, AWS SDK, Security, JWT 등)
- [ ] application.yml 기본 설정
- [ ] BaseTimeEntity 구현 (생성일시, 수정일시 자동 관리)
- [ ] GlobalExceptionHandler 구현 (전역 예외 처리)
- [ ] ErrorCode, BusinessException, ErrorResponse 구현
- [ ] EnumConverter 유틸리티 구현
- [ ] SwaggerConfig 설정 (API 문서화)

### 🏷️ 라벨
`setup`, `infrastructure`

### 📝 커밋 메시지
```
chore: 프로젝트 초기 설정 및 공통 유틸리티 추가
```

---

## Issue #2: JWT 기반 인증/인가 시스템 구현

### 📋 설명
사용자 로그인/회원가입 기능과 JWT 토큰 기반 인증 시스템을 구현합니다.

### ✅ 작업 내용
- [ ] User 엔티티 및 UserRole enum 구현
- [ ] UserRepository 구현
- [ ] JwtService 구현 (토큰 생성, 검증)
- [ ] AuthService 구현 (회원가입, 로그인)
- [ ] AuthController 구현 (POST /api/auth/signup, POST /api/auth/login)
- [ ] SignUpRequest, LoginRequest, AuthResponse DTO 구현
- [ ] SecurityConfig 구현 (JWT 필터, CORS 설정)
- [ ] PasswordEncoder 설정

### 🏷️ 라벨
`feature`, `auth`, `security`

### 📝 커밋 메시지
```
feat: JWT 기반 인증/인가 시스템 구현
```

---

## Issue #3: 이력서 관리 기능 구현

### 📋 설명
지원자의 이력서를 생성, 조회, 수정, 삭제하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] Resume 엔티티 구현
- [ ] AcademicRecord, JobPreference, WorkCondition 임베디드 엔티티 구현
- [ ] Gender, AcademicStatus, ExperienceLevel, EmploymentType enum 구현
- [ ] ResumeRepository 구현
- [ ] ResumeService 구현 (CRUD)
- [ ] ResumeController 구현
- [ ] ResumeRequest, ResumeUpdateRequest, ResumeResponse DTO 구현
- [ ] 이메일로 이력서 조회 기능 추가 (GET /api/v1/resumes/email/{email})

### 🏷️ 라벨
`feature`, `resume`

### 📝 커밋 메시지
```
feat: 이력서 관리 기능 구현
```

---

## Issue #4: 채용공고 관리 기능 구현

### 📋 설명
기업의 채용공고를 생성, 조회, 수정, 삭제하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] JobPosting 엔티티 구현
- [ ] JobPostingRepository 구현
- [ ] JobPostingService 구현 (CRUD)
- [ ] JobPostingController 구현
- [ ] JobPostingRequest, JobPostingUpdateRequest, JobPostingResponse DTO 구현
- [ ] JobPostingApiDocs 인터페이스 구현

### 🏷️ 라벨
`feature`, `job-posting`

### 📝 커밋 메시지
```
feat: 채용공고 관리 기능 구현
```

---

## Issue #5: 질문 관리 기능 및 공통 질문 초기화

### 📋 설명
면접 질문을 관리하고 애플리케이션 시작 시 공통 질문을 초기화하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] Question 엔티티 구현
- [ ] QuestionType enum 구현 (COMMON, JOB_POSTING, RESUME)
- [ ] QuestionRepository 구현
- [ ] QuestionService 구현
- [ ] DataInitializer 구현 (애플리케이션 시작 시 공통 질문 6개 자동 생성)
- [ ] QuestionResponse DTO 구현

### 🏷️ 라벨
`feature`, `question`

### 📝 커밋 메시지
```
feat: 질문 관리 기능 및 공통 질문 초기화 구현
```

---

## Issue #6: AWS Bedrock 서비스 통합

### 📋 설명
AWS Bedrock을 사용하여 AI 기반 질문 생성, 채점, 꼬리물기 응답 생성 기능을 구현합니다.

### ✅ 작업 내용
- [ ] BedrockService 구현 (꼬리물기 응답 생성)
- [ ] QuestionGenerationService 구현 (채용공고/이력서 기반 질문 자동 생성)
- [ ] InterviewScoringService 구현 (면접 답변 자동 채점)
- [ ] application.yml에 Bedrock 설정 추가
- [ ] 비동기 처리 설정 (@Async, TaskExecutor)
- [ ] Bedrock 비활성화 옵션 추가 (bedrock.enabled)

### 🏷️ 라벨
`feature`, `ai`, `bedrock`

### 📝 커밋 메시지
```
feat: AWS Bedrock 서비스 통합 (질문 생성, 채점, 꼬리물기)
```

---

## Issue #7: 면접 세션 관리 기능 구현

### 📋 설명
면접 세션을 생성하고 질문을 순차적으로 진행하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] InterviewSession 엔티티 구현
- [ ] InterviewQuestion 엔티티 구현 (세션별 질문 순서 관리)
- [ ] InterviewStatus enum 구현 (IN_PROGRESS, COMPLETED, SCORED, ERROR)
- [ ] InterviewSessionRepository, InterviewQuestionRepository 구현
- [ ] InterviewService 기본 로직 구현 (startInterview, handleAnswer)
- [ ] InterviewController 구현
- [ ] InterviewStartRequest, InterviewStartResponse DTO 구현
- [ ] InterviewAnswerRequest, InterviewAnswerResponse DTO 구현
- [ ] 중복 면접 방지 로직 추가

### 🏷️ 라벨
`feature`, `interview`

### 📝 커밋 메시지
```
feat: 면접 세션 관리 기능 구현
```

---

## Issue #8: 면접 답변 저장 및 자동 채점 기능 구현

### 📋 설명
면접 답변을 저장하고 면접 종료 시 자동으로 채점하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] InterviewAnswer 엔티티 구현
- [ ] InterviewScore 엔티티 구현
- [ ] InterviewAnswerRepository, InterviewScoreRepository 구현
- [ ] InterviewService에 답변 저장 로직 추가
- [ ] InterviewService에 면접 종료 시 자동 채점 트리거 추가
- [ ] InterviewScoringService와 연동
- [ ] 채점 완료 후 InterviewStatus 업데이트 (SCORED/ERROR)
- [ ] InterviewScoreResponse DTO 구현

### 🏷️ 라벨
`feature`, `interview`, `scoring`

### 📝 커밋 메시지
```
feat: 면접 답변 저장 및 자동 채점 기능 구현
```

---

## Issue #9: 면접 세션 및 답변 조회 API 추가

### 📋 설명
면접 세션 정보와 답변 목록을 조회하는 API를 추가합니다.

### ✅ 작업 내용
- [ ] InterviewService에 getSession 메서드 추가
- [ ] InterviewService에 getAnswers 메서드 추가
- [ ] InterviewService에 getSessionsByApplicant 메서드 추가
- [ ] InterviewService에 getSessionsByJobPosting 메서드 추가
- [ ] InterviewController에 조회 엔드포인트 추가
  - GET /interview/session/{sessionId}
  - GET /interview/session/{sessionId}/answers
  - GET /interview/sessions?applicantEmail={email}
  - GET /interview/sessions?jobPostingId={id}
- [ ] InterviewSessionResponse, InterviewAnswerDetailResponse DTO 구현

### 🏷️ 라벨
`feature`, `interview`, `api`

### 📝 커밋 메시지
```
feat: 면접 세션 및 답변 조회 API 추가
```

---

## Issue #10: 채용공고/이력서 기반 질문 자동 생성 연동

### 📋 설명
채용공고나 이력서 생성 시 자동으로 관련 질문을 생성하고 조회할 수 있는 기능을 추가합니다.

### ✅ 작업 내용
- [ ] JobPostingService에 질문 생성 연동 추가
- [ ] ResumeService에 질문 생성 연동 추가
- [ ] QuestionService에 findByJobPostingId, findByResumeId 메서드 추가
- [ ] QuestionRepository에 JobPosting/Resume별 질문 조회 메서드 추가
- [ ] JobPostingController에 질문 조회 엔드포인트 추가 (GET /api/v1/job-postings/{id}/questions)
- [ ] ResumeController에 질문 조회 엔드포인트 추가 (GET /api/v1/resumes/{id}/questions)

### 🏷️ 라벨
`feature`, `question`, `integration`

### 📝 커밋 메시지
```
feat: 채용공고/이력서 기반 질문 자동 생성 연동
```

---

## Issue #11: 지원자 랭킹 조회 기능 구현

### 📋 설명
특정 채용공고에 지원한 지원자들의 점수를 기준으로 랭킹을 조회하는 기능을 구현합니다.

### ✅ 작업 내용
- [ ] InterviewScore 엔티티에 랭킹 조회용 필드 추가 (jobPostingId, applicantEmail, applicantName)
- [ ] InterviewScoreRepository에 랭킹 조회 메서드 추가
- [ ] InterviewService에 랭킹 조회 메서드 추가
- [ ] InterviewController에 랭킹 조회 엔드포인트 추가
- [ ] RankingResponse DTO 구현

### 🏷️ 라벨
`feature`, `ranking`

### 📝 커밋 메시지
```
feat: 지원자 랭킹 조회 기능 구현
```

---

## Issue #12: Bedrock 연결 테스트 엔드포인트 추가

### 📋 설명
Bedrock 연결 상태를 확인할 수 있는 테스트 엔드포인트를 추가합니다.

### ✅ 작업 내용
- [ ] BedrockTestController 구현
- [ ] GET /api/test/bedrock 엔드포인트 추가
- [ ] Bedrock 연결 테스트 로직 구현
- [ ] SecurityConfig에 테스트 엔드포인트 허용 설정

### 🏷️ 라벨
`feature`, `test`, `bedrock`

### 📝 커밋 메시지
```
feat: Bedrock 연결 테스트 엔드포인트 추가
```

---

## Issue #13: 비동기 처리 시 LazyInitializationException 해결

### 📋 설명
비동기 메서드에서 Lazy 로딩된 엔티티에 접근할 때 발생하는 LazyInitializationException을 해결합니다.

### ✅ 작업 내용
- [ ] InterviewScoringService.scoreInterview 메서드 시그니처 변경 (JobPosting 객체 → Long jobPostingId)
- [ ] InterviewScoringService에 JobPostingRepository 의존성 추가
- [ ] InterviewService에서 scoreInterview 호출 시 ID만 전달하도록 수정
- [ ] InterviewScoringService에서 새 트랜잭션에서 JobPosting 조회하도록 수정

### 🏷️ 라벨
`bugfix`, `performance`

### 📝 커밋 메시지
```
fix: 비동기 처리 시 LazyInitializationException 해결
```

---

## Issue #14: AWS 자격 증명 설정 및 Bedrock 활성화 옵션 추가

### 📋 설명
로컬 개발 환경에서 Bedrock을 선택적으로 사용할 수 있도록 설정 옵션을 추가합니다.

### ✅ 작업 내용
- [ ] application-dev.yml에 bedrock.enabled 설정 추가
- [ ] BedrockService, QuestionGenerationService, InterviewScoringService에 bedrockEnabled 체크 추가
- [ ] Bedrock 비활성화 시 기본 응답 반환 로직 추가
- [ ] AWS 자격 증명 오류 시 명확한 에러 메시지 추가
- [ ] AWS_SETUP.md 문서 작성

### 🏷️ 라벨
`enhancement`, `configuration`

### 📝 커밋 메시지
```
feat: AWS Bedrock 활성화 옵션 및 자격 증명 설정 가이드 추가
```

---

## 이슈 작성 팁

1. **이슈 제목**: 명확하고 간결하게 작성
   - 예: `[Feature] JWT 기반 인증/인가 시스템 구현`

2. **라벨 사용**: 
   - `feature`: 새로운 기능
   - `bugfix`: 버그 수정
   - `enhancement`: 기능 개선
   - `setup`: 초기 설정
   - `documentation`: 문서화

3. **체크리스트**: 각 작업 항목을 체크리스트로 작성하여 진행 상황 추적

4. **연관 이슈**: 의존성이 있는 이슈는 "Related to #이슈번호"로 명시

5. **마일스톤**: 프로젝트 단계별로 마일스톤 설정 가능

