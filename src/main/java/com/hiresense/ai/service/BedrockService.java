package com.hiresense.ai.service;

import com.hiresense.ai.config.BedrockConfig;
import com.hiresense.ai.config.PromptProperties;
import com.hiresense.ai.dto.request.AnthropicInvokeModelRequest;
import com.hiresense.ai.dto.request.ContentBlock;
import com.hiresense.ai.dto.request.Message;
import com.hiresense.ai.util.BedrockErrorHandler;
import com.hiresense.ai.util.BedrockResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockService {

    private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final int MAX_TOKENS = 500;
    private static final String USER_ROLE = "user";
    private static final String CONTENT_TYPE_TEXT = "text";

    private final BedrockConfig bedrockConfig;
    private final PromptProperties promptProperties;
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final BedrockResponseParser responseParser;
    private final BedrockErrorHandler errorHandler;

    public String getTailBitingResponse(String previousAnswer, String nextQuestion) {
        String defaultResponse = String.format("좋은 답변입니다. 그럼 다음 질문입니다. %s", nextQuestion);
        
        if (!bedrockConfig.isBedrockEnabled() || bedrockRuntimeClient == null) {
            log.warn("[BedrockService] Bedrock이 비활성화되어 있습니다. 기본 연결 멘트를 사용합니다.");
            return defaultResponse;
        }

        String prompt = String.format(promptProperties.getTailBiting(), previousAnswer, nextQuestion);

        try {
            ContentBlock contentBlock = new ContentBlock(CONTENT_TYPE_TEXT, prompt);
            Message message = new Message(USER_ROLE, Collections.singletonList(contentBlock));

            AnthropicInvokeModelRequest bedrockRequest = AnthropicInvokeModelRequest.builder()
                    .anthropicVersion(ANTHROPIC_VERSION)
                    .maxTokens(MAX_TOKENS)
                    .messages(Collections.singletonList(message))
                    .build();

            String bodyJson = objectMapper.writeValueAsString(bedrockRequest);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(bedrockConfig.getModelId())
                    .body(SdkBytes.fromString(bodyJson, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            String tailBitingText = responseParser.extractTextFromResponse(responseBody);
            
            log.info("[BedrockService] 꼬리물기 생성 완료: {}", tailBitingText);
            return tailBitingText;
        } catch (Exception e) {
            return errorHandler.handleError(e, "[BedrockService] 꼬리물기 생성", defaultResponse);
        }
    }
}
