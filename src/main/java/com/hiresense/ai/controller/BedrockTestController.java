package com.hiresense.ai.controller;

import com.hiresense.ai.service.BedrockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class BedrockTestController {

    private final BedrockService bedrockService;

    @GetMapping("/bedrock")
    public ResponseEntity<Map<String, Object>> testBedrock() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("[Test] Bedrock 연결 테스트 시작");
            
            // 간단한 테스트 요청
            String testResponse = bedrockService.getTailBitingResponse(
                "저는 백엔드 개발자로 3년간 일했습니다.",
                "그렇다면, 가장 어려웠던 기술적 도전은 무엇이었나요?"
            );
            
            result.put("status", "success");
            result.put("message", "Bedrock 연결 성공");
            result.put("testResponse", testResponse);
            result.put("connected", true);
            
            log.info("[Test] Bedrock 연결 테스트 성공: {}", testResponse);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Bedrock 연결 실패: " + e.getMessage());
            result.put("connected", false);
            result.put("error", e.getClass().getSimpleName());
            
            log.error("[Test] Bedrock 연결 테스트 실패: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(result);
    }
}

