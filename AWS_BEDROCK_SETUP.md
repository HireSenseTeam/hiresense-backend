# AWS Bedrock 설정 가이드

## 필수 설정 단계

### 1. Bedrock 서비스 활성화 및 모델 접근 권한 설정

#### Step 1: AWS Console 접속
1. AWS Console (https://console.aws.amazon.com) 로그인
2. 리전을 **us-east-1 (N. Virginia)** 또는 **ap-northeast-1 (Tokyo)** 로 변경
   - Bedrock은 특정 리전에서만 사용 가능

#### Step 2: Bedrock 서비스 접근
1. AWS Console 검색창에서 **"Bedrock"** 검색
2. **Amazon Bedrock** 서비스 선택

#### Step 3: 모델 접근 권한 요청 (가장 중요!)
1. 왼쪽 메뉴에서 **"Model access"** 클릭
2. **"Request model access"** 버튼 클릭
3. 사용할 모델 선택:
   - **Anthropic Claude 3 Sonnet** (현재 사용 중: `anthropic.claude-3-sonnet-20240229-v1:0`)
   - 또는 **"Select all"** 클릭하여 모든 모델 요청
4. **"Submit request"** 클릭
5. **승인 대기** (보통 몇 분~몇 시간 소요)

**⚠️ 중요**: 모델 접근 권한이 없으면 Bedrock API 호출이 실패합니다!

### 2. IAM 권한 설정

#### Step 1: IAM Policy 생성
1. AWS Console → **IAM** → **Policies** → **Create policy**
2. **JSON** 탭 선택
3. 다음 정책 붙여넣기:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "bedrock:InvokeModel",
                "bedrock:InvokeModelWithResponseStream"
            ],
            "Resource": [
                "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "bedrock:GetGuardrail",
                "bedrock:ListGuardrails"
            ],
            "Resource": "*"
        }
    ]
}
```

4. **Policy name**: `BedrockInvokePolicy` 입력
5. **Create policy** 클릭

#### Step 2: IAM User에 Policy 연결
1. **IAM** → **Users** → 사용자 선택 (또는 새 사용자 생성)
2. **Add permissions** → **Attach policies directly**
3. 방금 생성한 `BedrockInvokePolicy` 선택
4. **Add permissions** 클릭

### 3. Guardrail 설정 (선택사항, 현재 사용 중)

현재 설정에서 Guardrail을 사용하고 있습니다:
- Guardrail ID: `gi0pmpvfbz8t`

Guardrail이 이미 설정되어 있다면 추가 작업이 필요 없습니다.
새로 만들려면:
1. Bedrock Console → **Guardrails** → **Create guardrail**
2. Guardrail 생성 후 ID를 `application.yml`에 설정

### 4. 설정 확인

#### AWS Console에서 확인
1. **Bedrock** → **Model access** → 모델이 **"Access granted"** 상태인지 확인
2. **IAM** → **Users** → 사용자 → **Permissions** → 정책이 연결되어 있는지 확인

#### 애플리케이션에서 확인
```bash
# 서버 실행 후
curl http://localhost:8000/api/test/bedrock
```

성공 응답:
```json
{
  "status": "success",
  "message": "Bedrock 연결 성공",
  "connected": true
}
```

## 문제 해결

### "AccessDeniedException" 오류
- **원인**: 모델 접근 권한이 없거나 IAM 권한이 부족
- **해결**: 
  1. Bedrock Console → Model access에서 모델 접근 권한 요청
  2. IAM 정책이 올바르게 연결되었는지 확인

### "Model not found" 오류
- **원인**: 모델 ID가 잘못되었거나 해당 리전에서 사용 불가
- **해결**: 
  1. Bedrock Console → Model access에서 사용 가능한 모델 확인
  2. `application.yml`의 `bedrock.model-id` 확인

### "Unable to load credentials" 오류
- **원인**: AWS 자격 증명이 설정되지 않음
- **해결**: `~/.aws/credentials` 파일 확인

## 현재 프로젝트 설정

- **모델**: `anthropic.claude-3-sonnet-20240229-v1:0`
- **리전**: `us-east-1`
- **Guardrail ID**: `gi0pmpvfbz8t`

## 체크리스트

- [ ] AWS Console에서 Bedrock 서비스 접근
- [ ] Model access에서 Anthropic Claude 3 Sonnet 접근 권한 요청 및 승인
- [ ] IAM Policy 생성 (`BedrockInvokePolicy`)
- [ ] IAM User에 Policy 연결
- [ ] `~/.aws/credentials` 파일에 자격 증명 설정
- [ ] 서버 재시작 후 `/api/test/bedrock` 엔드포인트로 테스트

