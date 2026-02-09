package com.hiresense.ai.service;

import com.hiresense.ai.config.BedrockConfig;
import com.hiresense.ai.config.PromptProperties;
import com.hiresense.ai.dto.request.AnthropicInvokeModelRequest;
import com.hiresense.ai.dto.request.ContentBlock;
import com.hiresense.ai.dto.request.Message;
import com.hiresense.ai.util.BedrockErrorHandler;
import com.hiresense.ai.util.BedrockResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;
import com.hiresense.interview.domain.InterviewAnswer;
import com.hiresense.interview.domain.InterviewScore;
import com.hiresense.interview.domain.InterviewSession;
import com.hiresense.interview.repository.InterviewAnswerRepository;
import com.hiresense.interview.repository.InterviewScoreRepository;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.jobPosting.repository.JobPostingRepository;
import com.hiresense.resume.domain.Resume;
import com.hiresense.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewScoringService {

    private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final int MAX_TOKENS_SCORING = 2000;
    private static final String USER_ROLE = "user";
    private static final String CONTENT_TYPE_TEXT = "text";

    private final BedrockConfig bedrockConfig;
    private final PromptProperties promptProperties;
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ResumeRepository resumeRepository;
    private final BedrockResponseParser responseParser;
    private final BedrockErrorHandler errorHandler;

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> scoreInterview(InterviewSession session, Long jobPostingId) {
        long scoringStartTime = System.currentTimeMillis();
        log.info("⚡ [비동기 채점 스레드] 채점 시작 (스레드: {}) - Session={}, Job={}, Applicant={}",
                Thread.currentThread().getName(), session.getId(), jobPostingId, session.getApplicantEmail());

        if (!bedrockConfig.isBedrockEnabled() || bedrockRuntimeClient == null) {
            log.warn("[InterviewScoringService] Bedrock이 비활성화되어 있습니다. 채점을 건너뜁니다.");
            return CompletableFuture.completedFuture(null);
        }

        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));

        Resume resume = resumeRepository.findByEmail(session.getApplicantEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        List<InterviewAnswer> allAnswers = interviewAnswerRepository.findBySessionId(session.getId());
        
        if (allAnswers.isEmpty()) {
            log.warn("[InterviewScoringService] 답변이 없어 채점을 건너뜁니다.");
            return CompletableFuture.completedFuture(null);
        }

        String answersFormattedText = allAnswers.stream()
                .map(answer -> String.format("Q (%s): %s\n", 
                        answer.getQuestion().getId(), 
                        answer.getAnswerText()))
                .collect(Collectors.joining());

        log.info("[InterviewScoringService] {}개의 답변 로드 완료.", allAnswers.size());

        String criteriaText = String.format("""
                인재상(idealCandidate): %s
                주요 업무(jobDescription): %s
                - 자격 요건(qualifications): %s
                """,
                jobPosting.getIdealCandidate() != null ? jobPosting.getIdealCandidate() : "N/A",
                jobPosting.getJobDescription() != null ? jobPosting.getJobDescription() : "N/A",
                jobPosting.getQualifications() != null ? jobPosting.getQualifications() : "N/A"
        );

        String prompt = String.format(promptProperties.getScoring(), criteriaText, answersFormattedText);

        Map<String, Object> finalReport;

        try {
            ContentBlock contentBlock = new ContentBlock(CONTENT_TYPE_TEXT, prompt);
            Message message = new Message(USER_ROLE, Collections.singletonList(contentBlock));

            AnthropicInvokeModelRequest bedrockRequest = new AnthropicInvokeModelRequest(
                    ANTHROPIC_VERSION,
                    MAX_TOKENS_SCORING,
                    Collections.singletonList(message)
            );

            String bodyJson = objectMapper.writeValueAsString(bedrockRequest);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(bedrockConfig.getModelId())
                    .body(SdkBytes.fromString(bodyJson, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            int maxRetries = 3;
            long initialDelay = 2000;
            InvokeModelResponse response = null;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    response = bedrockRuntimeClient.invokeModel(request);
                    break;
                } catch (software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException e) {
                    if (attempt >= maxRetries - 1) {
                        log.error("[InterviewScoringService] Bedrock 채점 실패 (최대 재시도 도달): {}", e.getMessage());
                        throw e;
                    }
                    long delay = initialDelay * (long) Math.pow(2, attempt);
                    log.warn("[InterviewScoringService] Bedrock API Throttling. {}ms 후 재시도... (시도 {}/{})", delay, attempt + 2, maxRetries);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ErrorCode.SCORING_FAILED, "재시도 중 스레드 인터럽트 발생");
                    }
                }
            }

            if (response == null) {
                throw new BusinessException(ErrorCode.SCORING_FAILED, "Bedrock으로부터 응답을 받지 못했습니다.");
            }

            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            String scoringResultText = responseParser.extractTextFromResponse(responseBody);
            finalReport = responseParser.parseJsonFromResponse(scoringResultText);

            log.info("[InterviewScoringService] Bedrock 채점 완료, 총점: {}", finalReport.get("overall_score"));
        } catch (Exception e) {
            log.error("[InterviewScoringService] Bedrock 채점 실패: {}", e.getMessage(), e);
            errorHandler.handleErrorAndThrow(e, "[InterviewScoringService] 채점");
            return CompletableFuture.failedFuture(new BusinessException(ErrorCode.SCORING_FAILED));
        }

        try {
            BigDecimal scoreDecimal = parseOverallScore(finalReport);
            int[] fitScores = parseSuitabilityScores(finalReport);

            InterviewScore score = InterviewScore.create(
                    session,
                    scoreDecimal,
                    (String) finalReport.get("overall_comment"),
                    (String) finalReport.get("strengths"),
                    (String) finalReport.get("weaknesses"),
                    fitScores[0], // idealCandidateFit
                    fitScores[1], // jobDescriptionFit
                    jobPosting.getId(),
                    session.getApplicantEmail(),
                    resume.getName()
            );

            interviewScoreRepository.save(score);
            long scoringElapsed = System.currentTimeMillis() - scoringStartTime;
            log.info("⚡ [비동기 채점 완료] 총 채점 소요 시간: {}ms ({}초) - Session={}, Score={}",
                    scoringElapsed, scoringElapsed / 1000.0, session.getId(), scoreDecimal);
            return CompletableFuture.completedFuture(null);
        } catch (BusinessException e) {
            log.error("[InterviewScoringService] 채점 결과 저장 실패: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("[InterviewScoringService] 채점 결과 저장 실패: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(new BusinessException(ErrorCode.SCORING_FAILED));
        }
    }

    private BigDecimal parseOverallScore(Map<String, Object> finalReport) {
        Object overallScoreObj = finalReport.get("overall_score");
        if (overallScoreObj == null) {
            throw new BusinessException(ErrorCode.SCORING_DATA_INVALID, "final_report에 'overall_score'가 없습니다.");
        }

        if (overallScoreObj instanceof Integer) {
            return new BigDecimal((Integer) overallScoreObj);
        } else if (overallScoreObj instanceof String) {
            String overallScoreStr = (String) overallScoreObj;
            String scoreNumber = overallScoreStr.split("점")[0].trim();
            return new BigDecimal(scoreNumber);
        } else if (overallScoreObj instanceof Number) {
            return new BigDecimal(((Number) overallScoreObj).doubleValue());
        } else {
            throw new BusinessException(ErrorCode.SCORING_DATA_INVALID, 
                    "overall_score 형식이 올바르지 않습니다: " + overallScoreObj.getClass());
        }
    }

    private int[] parseSuitabilityScores(Map<String, Object> finalReport) {
        @SuppressWarnings("unchecked")
        Map<String, Object> suitabilityScore = (Map<String, Object>) finalReport.get("suitability_score");
        
        if (suitabilityScore == null) {
            throw new BusinessException(ErrorCode.SCORING_DATA_INVALID, "final_report에 'suitability_score'가 없습니다.");
        }
        
        Object idealCandidateFitObj = suitabilityScore.get("ideal_candidate_fit");
        Object jobDescriptionFitObj = suitabilityScore.get("job_description_fit");
        
        int idealCandidateFit = parseIntegerValue(idealCandidateFitObj, "ideal_candidate_fit");
        int jobDescriptionFit = parseIntegerValue(jobDescriptionFitObj, "job_description_fit");
        
        return new int[]{idealCandidateFit, jobDescriptionFit};
    }

    private int parseIntegerValue(Object value, String fieldName) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt(((String) value).trim());
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            throw new BusinessException(ErrorCode.SCORING_DATA_INVALID, 
                    fieldName + " 형식이 올바르지 않습니다: " + (value != null ? value.getClass() : "null"));
        }
    }
}
