# Hiresense Backend

Spring Boot 기반 AI 면접 시스템 백엔드 서버

## 🚀 빠른 시작

### 1. 서버 실행

```bash
# 방법 1: 스크립트 사용
./start-server.sh

# 방법 2: Gradle 직접 실행
./gradlew bootRun

# 방법 3: JAR 파일 실행
./gradlew build
java -jar build/libs/hiresense-0.0.1-SNAPSHOT.jar
```

### 2. 서버 주소 확인

서버가 실행되면 다음 주소로 접근 가능합니다:

- **로컬 접근**: `http://localhost:8000`
- **네트워크 접근**: `http://192.168.35.27:8000` (로컬 IP는 변경될 수 있음)
- **Swagger UI**: `http://localhost:8000/swagger-ui.html`

## 📱 Expo Go로 테스트하기

### 1. 서버 실행 확인

```bash
# 서버가 실행 중인지 확인
curl http://localhost:8000/swagger-ui.html
```

### 2. 로컬 IP 주소 확인

```bash
# macOS/Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# 또는
./start-server.sh  # 스크립트가 자동으로 IP를 표시합니다
```

### 3. Expo Go 앱에서 API 호출

Expo Go 앱(React Native)에서 다음과 같이 API를 호출하세요:

```javascript
// API Base URL 설정
const API_BASE_URL = 'http://192.168.35.27:8000';  // 본인의 로컬 IP로 변경

// 예시: 면접 시작
const startInterview = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/interview/start`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        jobId: 1,
        applicantEmail: 'test@example.com'
      })
    });
    const data = await response.json();
    console.log('면접 시작:', data);
  } catch (error) {
    console.error('에러:', error);
  }
};
```

### 4. 네트워크 요구사항

- **같은 Wi-Fi 네트워크**: Expo Go 앱과 서버가 같은 Wi-Fi에 연결되어 있어야 합니다
- **방화벽 설정**: macOS 방화벽이 포트 8000을 차단하지 않는지 확인하세요
- **CORS**: 이미 모든 origin을 허용하도록 설정되어 있습니다

## 📋 주요 API 엔드포인트

### Interview (면접)
- `POST /interview/start` - 면접 시작
- `POST /interview/answer` - 답변 제출
- `GET /interview/score?sessionId={id}` - 점수 조회
- `GET /interview/session/{sessionId}` - 세션 조회
- `GET /interview/session/{sessionId}/answers` - 답변 목록
- `GET /interview/sessions?applicantEmail={email}` - 지원자별 세션 목록

### JobPosting (채용공고)
- `POST /api/v1/job-postings` - 생성
- `GET /api/v1/job-postings` - 전체 조회
- `GET /api/v1/job-postings/{id}` - 단건 조회
- `GET /api/v1/job-postings/{id}/questions` - 질문 조회

### Resume (이력서)
- `POST /api/v1/resumes` - 생성
- `GET /api/v1/resumes/{id}` - ID로 조회
- `GET /api/v1/resumes/email/{email}` - 이메일로 조회
- `GET /api/v1/resumes/{id}/questions` - 질문 조회

### Ranking (랭킹)
- `GET /api/rankings/{jobId}` - 랭킹 조회

## 🔧 설정

### 데이터베이스 설정

`application-dev.yml`에서 로컬 MySQL 설정을 확인하세요:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hiresense
    username: root
    password: 1234  # 본인의 MySQL 비밀번호로 변경
```

### 환경 변수

- `DB_PASSWORD`: MySQL 비밀번호 (선택사항)

## 🐛 문제 해결

### Expo Go에서 연결이 안 될 때

1. **같은 Wi-Fi 확인**: 서버와 모바일 기기가 같은 네트워크에 있는지 확인
2. **IP 주소 확인**: `./start-server.sh`로 현재 IP 확인
3. **방화벽 확인**: macOS 시스템 설정 > 보안 및 개인 정보 보호 > 방화벽
4. **서버 실행 확인**: `curl http://localhost:8000/swagger-ui.html`

### 포트 충돌

포트 8000이 사용 중이면 `application.yml`에서 포트를 변경하세요:

```yaml
server:
  port: 8001  # 다른 포트로 변경
```

## 📚 Swagger UI

서버 실행 후 브라우저에서 다음 주소로 접근:
- `http://localhost:8000/swagger-ui.html`

모든 API를 테스트하고 문서를 확인할 수 있습니다.
