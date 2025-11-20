# AWS Bedrock 설정 가이드

## 1. 환경 변수 설정

### 방법 1: 터미널에서 직접 설정 (임시)

서버를 실행하는 터미널에서 다음 명령어를 실행하세요:

```bash
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
export AWS_REGION=us-east-1

# 그 다음 서버 실행
./gradlew bootRun
```

**주의**: 이 방법은 터미널을 닫으면 환경 변수가 사라집니다.

### 방법 2: ~/.bashrc 또는 ~/.zshrc에 추가 (영구)

터미널 설정 파일에 추가하면 매번 자동으로 로드됩니다:

```bash
# ~/.zshrc 또는 ~/.bashrc 파일에 추가
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
export AWS_REGION=us-east-1
```

그 다음:
```bash
source ~/.zshrc  # 또는 source ~/.bashrc
```

### 방법 3: AWS credentials 파일 사용 (권장)

1. `~/.aws/credentials` 파일 생성:
```bash
mkdir -p ~/.aws
nano ~/.aws/credentials
```

2. 파일 내용:
```ini
[default]
aws_access_key_id = your-access-key-id
aws_secret_access_key = your-secret-access-key
```

3. `~/.aws/config` 파일 생성:
```bash
nano ~/.aws/config
```

4. 파일 내용:
```ini
[default]
region = us-east-1
```

### 방법 4: IntelliJ IDEA에서 설정

1. Run/Debug Configurations 열기
2. Environment variables 섹션에 추가:
   - `AWS_ACCESS_KEY_ID=your-access-key-id`
   - `AWS_SECRET_ACCESS_KEY=your-secret-access-key`
   - `AWS_REGION=us-east-1`

## 2. IAM 권한 설정

### 필요한 IAM 권한

Bedrock을 사용하려면 다음 권한이 필요합니다:

#### 최소 권한 (권장)
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
        }
    ]
}
```

#### 더 넓은 권한 (개발용)
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "bedrock:InvokeModel",
                "bedrock:InvokeModelWithResponseStream",
                "bedrock:ListFoundationModels",
                "bedrock:GetFoundationModel"
            ],
            "Resource": "*"
        }
    ]
}
```

### IAM 정책 생성 및 사용자에 연결

1. AWS Console → IAM → Policies → Create policy
2. JSON 탭에서 위의 정책 중 하나를 붙여넣기
3. Policy name: `BedrockInvokePolicy` (또는 원하는 이름)
4. Create policy 클릭

5. IAM → Users → 사용자 선택 → Add permissions → Attach policies directly
6. 방금 생성한 정책 선택 → Add permissions

### Guardrail 사용 시 추가 권한

Guardrail을 사용하는 경우 다음 권한도 필요합니다:

```json
{
    "Version": "2012-10-17",
    "Statement": [
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

## 3. 연결 확인

서버 실행 후 다음 명령어로 연결을 확인하세요:

```bash
curl http://localhost:8000/api/test/bedrock
```

성공 응답 예시:
```json
{
  "status": "success",
  "message": "Bedrock 연결 성공",
  "testResponse": "...",
  "connected": true
}
```

## 4. 문제 해결

### 자격 증명 오류가 발생하는 경우

1. 환경 변수가 제대로 설정되었는지 확인:
```bash
echo $AWS_ACCESS_KEY_ID
echo $AWS_SECRET_ACCESS_KEY
echo $AWS_REGION
```

2. AWS credentials 파일 확인:
```bash
cat ~/.aws/credentials
cat ~/.aws/config
```

3. IAM 권한 확인:
   - IAM 사용자에 Bedrock 권한이 있는지 확인
   - 정책이 올바르게 연결되었는지 확인

### Region 오류가 발생하는 경우

Bedrock은 특정 리전에서만 사용 가능합니다:
- `us-east-1` (N. Virginia) ✅
- `us-west-2` (Oregon) ✅
- `ap-southeast-1` (Singapore) ✅
- `ap-northeast-1` (Tokyo) ✅
- `eu-central-1` (Frankfurt) ✅

현재 설정된 리전: `us-east-1`

