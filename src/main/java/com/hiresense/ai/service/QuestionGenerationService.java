package com.hiresense.ai.service;

import com.hiresense.ai.config.BedrockConfig;
import com.hiresense.ai.config.PromptProperties;
import com.hiresense.ai.dto.request.AnthropicInvokeModelRequest;
import com.hiresense.ai.dto.request.ContentBlock;
import com.hiresense.ai.dto.request.Message;
import com.hiresense.ai.util.BedrockErrorHandler;
import com.hiresense.ai.util.BedrockResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.repository.QuestionRepository;
import com.hiresense.resume.domain.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final int MAX_TOKENS_JOB_POSTING = 1000;
    private static final int MAX_TOKENS_RESUME = 500;
    private static final String USER_ROLE = "user";
    private static final String CONTENT_TYPE_TEXT = "text";

    private final BedrockConfig bedrockConfig;
    private final PromptProperties promptProperties;
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;
    private final BedrockResponseParser responseParser;
    private final BedrockErrorHandler errorHandler;

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<List<Question>> generateJobPostingQuestions(JobPosting jobPosting) {
        log.info("[QuestionGenerationService] 채용공고 질문 생성을 시작합니다. JobPosting ID: {}", jobPosting.getId());

        if (!bedrockConfig.isBedrockEnabled() || bedrockRuntimeClient == null) {
            log.warn("[QuestionGenerationService] Bedrock이 비활성화되어 있습니다. 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String idealCandidateText = jobPosting.getIdealCandidate();
        if (idealCandidateText == null || idealCandidateText.isBlank()) {
            log.warn("[QuestionGenerationService] 'idealCandidate' 필드가 없어 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String prompt = String.format(promptProperties.getJobPostingQuestion(), idealCandidateText);

        List<Question> generatedQuestions = new ArrayList<>();

        try {
            ContentBlock contentBlock = new ContentBlock(CONTENT_TYPE_TEXT, prompt);
            Message message = new Message(USER_ROLE, Collections.singletonList(contentBlock));

            AnthropicInvokeModelRequest bedrockRequest = AnthropicInvokeModelRequest.builder()
                    .anthropicVersion(ANTHROPIC_VERSION)
                    .maxTokens(MAX_TOKENS_JOB_POSTING)
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
            String generatedText = responseParser.extractTextFromResponse(responseBody);
            List<Map<String, String>> questionsData = responseParser.parseJsonArray(generatedText);

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

            log.info("[QuestionGenerationService] Bedrock이 생성한 채용공고 질문: {}개", generatedQuestions.size());
        } catch (Exception e) {
            errorHandler.handleError(e, "[QuestionGenerationService] 채용공고 질문 생성", null);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        if (!generatedQuestions.isEmpty()) {
            questionRepository.saveAll(generatedQuestions);
            log.info("[QuestionGenerationService] 채용공고 질문 저장 완료: {}개", generatedQuestions.size());
        } else {
            log.warn("[QuestionGenerationService] 채용공고 질문 생성 결과 없음.");
        }

        return CompletableFuture.completedFuture(generatedQuestions);
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<List<Question>> generateResumeQuestions(Resume resume) {
        log.info("[QuestionGenerationService] 이력서 질문 생성을 시작합니다. Resume ID: {}", resume.getId());

        if (!bedrockConfig.isBedrockEnabled() || bedrockRuntimeClient == null) {
            log.warn("[QuestionGenerationService] Bedrock이 비활성화되어 있습니다. 질문 생성을 건너뜁니다.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        try {
            String actualMajor = resume.getAcademicRecord() != null && resume.getAcademicRecord().getMajor() != null
                    ? resume.getAcademicRecord().getMajor() : "알 수 없음";
            String actualDesiredJob = resume.getJobPreference() != null && resume.getJobPreference().getDesiredJob() != null
                    ? resume.getJobPreference().getDesiredJob() : "알 수 없음";
            String actualExperience = resume.getJobPreference() != null && resume.getJobPreference().getExperienceLevel() != null
                    ? resume.getJobPreference().getExperienceLevel().toString() : "알 수 없음";

            String resumeJson = objectMapper.writeValueAsString(resume);

            String prompt = String.format(
                    promptProperties.getResumeQuestion(),
                    resumeJson, actualMajor, actualDesiredJob, actualExperience, actualMajor, actualDesiredJob
            );

            List<Question> generatedQuestions = new ArrayList<>();

            ContentBlock contentBlock = new ContentBlock(CONTENT_TYPE_TEXT, prompt);
            Message message = new Message(USER_ROLE, Collections.singletonList(contentBlock));

            AnthropicInvokeModelRequest bedrockRequest = AnthropicInvokeModelRequest.builder()
                    .anthropicVersion(ANTHROPIC_VERSION)
                    .maxTokens(MAX_TOKENS_RESUME)
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
            String generatedText = responseParser.extractTextFromResponse(responseBody);
            List<Map<String, String>> questionsData = responseParser.parseJsonArray(generatedText);

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

            log.info("[QuestionGenerationService] Bedrock이 생성한 이력서 질문: {}개", generatedQuestions.size());

            if (!generatedQuestions.isEmpty()) {
                questionRepository.saveAll(generatedQuestions);
                log.info("[QuestionGenerationService] 이력서 질문 저장 완료: {}개", generatedQuestions.size());
            } else {
                log.warn("[QuestionGenerationService] 이력서 질문 생성 결과 없음.");
            }

            return CompletableFuture.completedFuture(generatedQuestions);
        } catch (Exception e) {
            errorHandler.handleError(e, "[QuestionGenerationService] 이력서 질문 생성", null);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
}
