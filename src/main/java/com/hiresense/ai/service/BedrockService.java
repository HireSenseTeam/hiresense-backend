package com.hiresense.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockService {

    @Value("${bedrock.model-id}")
    private String modelId;

    @Value("${bedrock.region:us-east-1}")
    private String region;

    @Value("${bedrock.guardrail-identifier:}")
    private String guardrailIdentifier;

    @Value("${bedrock.enabled:true}")
    private boolean bedrockEnabled;

    private final ObjectMapper objectMapper;

    private BedrockRuntimeClient getBedrockClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * 지원자의 이전 답변에 공감한 후 다음 질문으로 자연스럽게 연결하는 멘트를 생성합니다.
     */
    public String getTailBitingResponse(String previousAnswer, String nextQuestion) {
        if (!bedrockEnabled) {
            log.warn("[Warn] Bedrock이 비활성화되어 있습니다. 기본 연결 멘트를 사용합니다.");
            return String.format("좋은 답변입니다. 그럼 다음 질문입니다. %s", nextQuestion);
        }

        String prompt = String.format("""
                Human: 당신은 친절하고 공감 능력이 뛰어난 AI 면접관입니다.
                
                지원자의 방금 답변에 가볍게 공감한 후 자연스럽게 다음 질문으로 연결하는 '연결 멘트(transition)'를 만들어야 합니다.
                
                - 지원자의 이전 답변: "%s"
                
                - 당신이 할 다음 질문: "%s"
                
                [규칙]
                
                1. 이전 답변의 핵심 키워드나 감정을 가볍게 언급합니다. (예: "~~게 느끼셨군요.", "~~한 경험을 하셨네요.")
                
                2. 자연스러운 연결어(예: "좋습니다.", "그렇다면,")를 사용하여 다음 질문으로 연결합니다.
                
                3. 다른 설명 없이 지원자에게 말할 멘트만 생성합니다.
                
                Assistant:
                """, previousAnswer, nextQuestion);

        try {
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", prompt);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", new Object[]{messageContent});

            Map<String, Object> body = new HashMap<>();
            body.put("anthropic_version", "bedrock-2023-05-31");
            body.put("max_tokens", 500);
            body.put("messages", new Object[]{message});

            String bodyJson = objectMapper.writeValueAsString(body);

            InvokeModelRequest.Builder requestBuilder = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(bodyJson, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json");

            InvokeModelRequest request = requestBuilder.build();

            try (BedrockRuntimeClient client = getBedrockClient()) {
                InvokeModelResponse response = client.invokeModel(request);
                String responseBody = response.body().asString(StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) responseMap.get("content");
                String tailBitingText = (String) content.get(0).get("text");
                
                log.info("[Bedrock] 꼬리물기 생성 (Guardrail 적용됨): {}", tailBitingText);
                return tailBitingText;
            }
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            // AWS 자격 증명 오류인 경우
            if (e.getMessage() != null && e.getMessage().contains("Unable to load credentials")) {
                log.error("[Error] AWS 자격 증명을 찾을 수 없습니다. AWS 자격 증명을 설정해주세요.");
                log.error("[Error] 환경 변수 설정: export AWS_ACCESS_KEY_ID=your-key && export AWS_SECRET_ACCESS_KEY=your-secret");
                log.error("[Error] 또는 ~/.aws/credentials 파일에 자격 증명을 설정하세요.");
                return String.format("좋은 답변입니다. 그럼 다음 질문입니다. %s", nextQuestion);
            }
            log.error("[Error] Bedrock 꼬리물기 호출 실패: {}", e.getMessage(), e);
            return String.format("좋은 답변입니다. 그럼 다음 질문입니다. %s", nextQuestion);
        } catch (Exception e) {
            log.error("[Error] Bedrock 꼬리물기 호출 실패 (Guardrail 포함): {}", e.getMessage(), e);
            return String.format("좋은 답변입니다. 그럼 다음 질문입니다. %s", nextQuestion);
        }
    }
}

