package com.hiresense.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.repository.QuestionRepository;
import com.hiresense.resume.domain.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    @Value("${bedrock.model-id}")
    private String modelId;

    @Value("${bedrock.region:us-east-1}")
    private String region;

    @Value("${bedrock.enabled:true}")
    private boolean bedrockEnabled;

    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;

    private BedrockRuntimeClient getBedrockClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * 채용공고 기반 질문 생성 (3개)
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<List<Question>> generateJobPostingQuestions(JobPosting jobPosting) {
        log.info("[Info] 채용 공고 질문 생성을 시작합니다. JobPosting ID: {}", jobPosting.getId());

        if (!bedrockEnabled) {
            log.warn("[Warn] Bedrock이 비활성화되어 있습니다. 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String idealCandidateText = jobPosting.getIdealCandidate();
        if (idealCandidateText == null || idealCandidateText.isBlank()) {
            log.warn("[Warn] 'idealCandidate' 필드가 없어 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String prompt = String.format("""
                Human: 다음은 우리 회사의 인재상(idealCandidate) 설명입니다.
                
                <idealCandidate>
                %s
                </idealCandidate>
                
                이 인재상을 바탕으로 지원자가 이에 부합하는지 확인할 수 있는 구체적인 경험 기반의 면접 질문 3가지를 생성해 주세요.
                질문은 반드시 다음 JSON 배열 형식으로만 대답해 주세요. 다른 설명은 모두 제외하고 JSON 코드만 반환해야 합니다.
                [
                { "question": "첫 번째 질문 내용" },
                { "question": "두 번째 질문 내용" },
                { "question": "세 번째 질문 내용" }
                ]
                
                Assistant:
                """, idealCandidateText);

        List<Question> generatedQuestions = new ArrayList<>();

        try {
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", prompt);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", new Object[]{messageContent});

            Map<String, Object> body = new HashMap<>();
            body.put("anthropic_version", "bedrock-2023-05-31");
            body.put("max_tokens", 1000);
            body.put("messages", new Object[]{message});

            String bodyJson = objectMapper.writeValueAsString(body);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(bodyJson, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            try (BedrockRuntimeClient client = getBedrockClient()) {
                InvokeModelResponse response = client.invokeModel(request);
                String responseBody = response.body().asString(StandardCharsets.UTF_8);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                String generatedText = (String) content.get(0).get("text");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> questionsData = objectMapper.readValue(generatedText, List.class);

                if (questionsData != null && !questionsData.isEmpty()) {
                    for (Map<String, String> qData : questionsData) {
                        String questionText = qData.get("question");
                        if (questionText != null && !questionText.isBlank()) {
                            Question question = Question.builder()
                                    .text(questionText)
                                    .type(QuestionType.JOB_POSTING)
                                    .jobPosting(jobPosting)
                                    .build();
                            generatedQuestions.add(question);
                        }
                    }
                }

                log.info("[Info] Bedrock이 생성한 채용 공고 질문: {}개", generatedQuestions.size());
            }
        } catch (Exception e) {
            log.error("[Error] Bedrock 채용 공고 질문 생성 실패: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        if (!generatedQuestions.isEmpty()) {
            questionRepository.saveAll(generatedQuestions);
            log.info("[Success] 채용 공고 질문 저장 완료: {}개", generatedQuestions.size());
        } else {
            log.warn("[Warn] 채용 공고 질문 생성 결과 없음.");
        }

        return CompletableFuture.completedFuture(generatedQuestions);
    }

    /**
     * 이력서 기반 질문 생성 (2개)
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<List<Question>> generateResumeQuestions(Resume resume) {
        log.info("[Info] 이력서 질문 생성을 시작합니다. Resume ID: {}", resume.getId());

        if (!bedrockEnabled) {
            log.warn("[Warn] Bedrock이 비활성화되어 있습니다. 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        try {
            String actualMajor = resume.getAcademicRecord() != null && resume.getAcademicRecord().getMajor() != null
                    ? resume.getAcademicRecord().getMajor() : "알 수 없음";
            String actualDesiredJob = resume.getJobPreference() != null && resume.getJobPreference().getDesiredJob() != null
                    ? resume.getJobPreference().getDesiredJob() : "알 수 없음";
            String actualExperience = resume.getJobPreference() != null && resume.getJobPreference().getExperienceLevel() != null
                    ? resume.getJobPreference().getExperienceLevel().toString() : "알 수 없음";

            // 이력서를 JSON 문자열로 변환
            String resumeJson = objectMapper.writeValueAsString(resume);

            String prompt = String.format("""
                    Human: 당신은 지원자를 평가하는 면접관입니다. 다음은 지원자의 이력서 내용입니다.
                    
                    <resume_content>
                    %s
                    </resume_content>
                    
                    이 지원자는 **%s을(를) 전공**했으며 **%s(%s)**을(를) 희망하고 있습니다.
                    이력서에 구체적인 프로젝트나 경력 사항은 부족할 수 있습니다.
                    지원자의 **전공 지식, 학습 능력, 문제 해결 능력, 성장 가능성** 등을 파악할 수 있는 **기본적이면서도 의미 있는 질문 2개**를 생성해주세요.
                    
                    [규칙]
                    1. 자기소개, 강점/약점 같은 너무 일반적인 질문은 제외합니다.
                    2. **학력(%s)**이나 **희망 직무(%s)**와 관련된 질문을 우선적으로 고려합니다.
                    3. 질문 앞에 번호 (1. 2.)를 붙여주세요.
                    4. 질문 외 다른 설명은 하지 마세요.
                    5. 반드시 다음 JSON 배열 형식으로만 대답해 주세요. 다른 설명은 모두 제외하고 JSON 코드만 반환해야 합니다.
                    
                    [
                    { "id": "q_resume_1", "text": "첫 번째 질문 내용" },
                    { "id": "q_resume_2", "text": "두 번째 질문 내용" }
                    ]
                    
                    Assistant:
                    """, resumeJson, actualMajor, actualDesiredJob, actualExperience, actualMajor, actualDesiredJob);

            List<Question> generatedQuestions = new ArrayList<>();

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

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(bodyJson, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            try (BedrockRuntimeClient client = getBedrockClient()) {
                InvokeModelResponse response = client.invokeModel(request);
                String responseBody = response.body().asString(StandardCharsets.UTF_8);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                String generatedText = (String) content.get(0).get("text");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> questionsData = objectMapper.readValue(generatedText, List.class);

                if (questionsData != null && !questionsData.isEmpty()) {
                    for (Map<String, String> qData : questionsData) {
                        String questionText = qData.get("text");
                        if (questionText != null && !questionText.isBlank()) {
                            Question question = Question.builder()
                                    .text(questionText)
                                    .type(QuestionType.RESUME)
                                    .resume(resume)
                                    .build();
                            generatedQuestions.add(question);
                        }
                    }
                }

                log.info("[Info] Bedrock이 생성한 이력서 질문: {}개", generatedQuestions.size());
            }

            if (!generatedQuestions.isEmpty()) {
                questionRepository.saveAll(generatedQuestions);
                log.info("[Success] 이력서 질문 저장 완료: {}개", generatedQuestions.size());
            } else {
                log.warn("[Warn] 이력서 질문 생성 결과 없음.");
            }

            return CompletableFuture.completedFuture(generatedQuestions);
        } catch (Exception e) {
            log.error("[Error] Bedrock 이력서 질문 생성 실패: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
}

