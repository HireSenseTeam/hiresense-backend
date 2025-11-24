package com.hiresense.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockResponseParser {

    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public String extractTextFromResponse(String responseBody) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            if (content == null || content.isEmpty()) {
                throw new IllegalArgumentException("Bedrock 응답에 content가 없습니다.");
            }
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.error("[BedrockResponseParser] 텍스트 추출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock 응답 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJsonFromResponse(String responseText) {
        try {
            String jsonPart;
            if (responseText.contains("```json")) {
                jsonPart = responseText.split("```json")[1].split("```")[0].trim();
            } else if (responseText.contains("```")) {
                jsonPart = responseText.split("```")[1].split("```")[0].trim();
            } else if (responseText.trim().startsWith("{")) {
                jsonPart = responseText.trim();
            } else {
                throw new IllegalArgumentException("Bedrock 응답에서 JSON 형식을 찾을 수 없습니다.");
            }

            return objectMapper.readValue(jsonPart, Map.class);
        } catch (Exception e) {
            log.error("[BedrockResponseParser] JSON 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock JSON 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> parseJsonArray(String jsonText) {
        try {
            return objectMapper.readValue(jsonText, List.class);
        } catch (Exception e) {
            log.error("[BedrockResponseParser] JSON 배열 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("JSON 배열 파싱 실패", e);
        }
    }
}
