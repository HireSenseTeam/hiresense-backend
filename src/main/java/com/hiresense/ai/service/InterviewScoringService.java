package com.hiresense.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewScoringService {

    @Value("${bedrock.model-id}")
    private String modelId;

    @Value("${bedrock.region:us-east-1}")
    private String region;

    @Value("${bedrock.enabled:true}")
    private boolean bedrockEnabled;

    private final ObjectMapper objectMapper;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ResumeRepository resumeRepository;

    private BedrockRuntimeClient getBedrockClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> scoreInterview(InterviewSession session, Long jobPostingId) {
        log.info("[Info] 채점 시작: Session={}, Job={}, Applicant={}", 
                session.getId(), jobPostingId, session.getApplicantEmail());

        if (!bedrockEnabled) {
            log.warn("[Warn] Bedrock이 비활성화되어 있습니다. 채점을 건너뜁니다.");
            return CompletableFuture.completedFuture(null);
        }

        // JobPosting을 새 트랜잭션에서 조회 (LazyInitializationException 방지)
        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new RuntimeException("JobPosting을 찾을 수 없습니다: " + jobPostingId));

        // Resume을 새 트랜잭션에서 조회 (LazyInitializationException 방지)
        Resume resume = resumeRepository.findByEmail(session.getApplicantEmail())
                .orElseThrow(() -> new RuntimeException("Resume을 찾을 수 없습니다: " + session.getApplicantEmail()));

        // 모든 답변 조회
        List<InterviewAnswer> allAnswers = interviewAnswerRepository.findBySessionId(session.getId());
        
        if (allAnswers.isEmpty()) {
            log.warn("[Warn] 답변이 없어 채점을 건너뜁니다.");
            return CompletableFuture.completedFuture(null);
        }

        // 답변 포맷팅
        String answersFormattedText = allAnswers.stream()
                .map(answer -> String.format("Q (%s): %s\n", 
                        answer.getQuestion().getId(), 
                        answer.getAnswerText()))
                .collect(Collectors.joining());

        log.info("[Info] {}개의 답변 로드 완료.", allAnswers.size());

        // 채용공고 기준 텍스트 생성
        String criteriaText = String.format("""
                인재상(idealCandidate): %s
                주요 업무(jobDescription): %s
                - 자격 요건(qualifications): %s
                """,
                jobPosting.getIdealCandidate() != null ? jobPosting.getIdealCandidate() : "N/A",
                jobPosting.getJobDescription() != null ? jobPosting.getJobDescription() : "N/A",
                jobPosting.getQualifications() != null ? jobPosting.getQualifications() : "N/A"
        );

        // Bedrock 채점 프롬프트
        String prompt = String.format("""
                Human: 당신은 채용 공고와 지원자의 면접 답변을 분석하여 평가 점수를 매기는 전문 HR 평가자입니다.
                아래 <채용공고 기준>과 <지원자 답변>을 **엄격하게 비교**하여 요청된 JSON 형식에 맞춰 **점수와 평가 의견**을 작성해야 합니다.
                **절대 다른 설명이나 대화 없이 오직 요청된 JSON 구조만 출력해야 합니다.**
                
                <채용공고 기준>
                %s
                </채용공고 기준>
                
                <지원자 답변>
                %s
                </지원자 답변>
                
                [출력 형식]
                **반드시 다음 JSON 형식에 맞춰 모든 필드를 채워서 응답하세요:**
                {
                "overall_score": "100점 만점 기준 총점 (숫자만 입력, 예: 85)",
                "overall_comment": "채용 기준 대비 지원자 답변에 대한 1~2줄 요약 평가.",
                "strengths": "채용 기준과 비교 시 지원자의 강점 1~2가지 요약.",
                "weaknesses": "채용 기준과 비교 시 지원자의 약점 또는 부족한 점 1~2가지 요약.",
                "suitability_score": {
                "ideal_candidate_fit": "인재상 적합도 점수 (1점에서 5점 사이 숫자만 입력)",
                "job_description_fit": "직무 적합도 점수 (1점에서 5점 사이 숫자만 입력)"
                }
                }
                
                Assistant:
                """, criteriaText, answersFormattedText);

        Map<String, Object> finalReport = null;

        try {
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", prompt);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", new Object[]{messageContent});

            Map<String, Object> body = new HashMap<>();
            body.put("anthropic_version", "bedrock-2023-05-31");
            body.put("max_tokens", 2000);
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
                String scoringResultText = (String) content.get(0).get("text");

                // JSON 파싱 (마크다운 제거)
                String jsonPart;
                if (scoringResultText.contains("```json")) {
                    jsonPart = scoringResultText.split("```json")[1].split("```")[0].trim();
                } else if (scoringResultText.trim().startsWith("{")) {
                    jsonPart = scoringResultText.trim();
                } else {
                    throw new IllegalArgumentException("Bedrock 응답에서 JSON 형식을 찾을 수 없습니다.");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> parsedReport = objectMapper.readValue(jsonPart, Map.class);
                finalReport = parsedReport;

                log.info("[Info] Bedrock 채점 완료, 총점: {}", finalReport.get("overall_score"));
            }
        } catch (Exception e) {
            log.error("[Error] Bedrock 채점 호출 또는 JSON 파싱 오류: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock 채점 오류", e);
        }

        // InterviewScore 저장
        try {
            Object overallScoreObj = finalReport.get("overall_score");
            if (overallScoreObj == null) {
                throw new IllegalArgumentException("final_report에 'overall_score'가 없습니다.");
            }

            // overall_score가 String 또는 Integer일 수 있으므로 처리
            BigDecimal scoreDecimal;
            if (overallScoreObj instanceof Integer) {
                scoreDecimal = new BigDecimal((Integer) overallScoreObj);
            } else if (overallScoreObj instanceof String) {
                String overallScoreStr = (String) overallScoreObj;
                // 점수 문자열에서 숫자만 추출
                String scoreNumber = overallScoreStr.split("점")[0].trim();
                scoreDecimal = new BigDecimal(scoreNumber);
            } else if (overallScoreObj instanceof Number) {
                scoreDecimal = new BigDecimal(((Number) overallScoreObj).doubleValue());
            } else {
                throw new IllegalArgumentException("overall_score 형식이 올바르지 않습니다: " + overallScoreObj.getClass());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> suitabilityScore = (Map<String, Object>) finalReport.get("suitability_score");
            
            // ideal_candidate_fit과 job_description_fit이 String 또는 Integer일 수 있음
            Object idealCandidateFitObj = suitabilityScore.get("ideal_candidate_fit");
            Object jobDescriptionFitObj = suitabilityScore.get("job_description_fit");
            
            int idealCandidateFit;
            int jobDescriptionFit;
            
            if (idealCandidateFitObj instanceof Integer) {
                idealCandidateFit = (Integer) idealCandidateFitObj;
            } else if (idealCandidateFitObj instanceof String) {
                idealCandidateFit = Integer.parseInt(((String) idealCandidateFitObj).trim());
            } else if (idealCandidateFitObj instanceof Number) {
                idealCandidateFit = ((Number) idealCandidateFitObj).intValue();
            } else {
                throw new IllegalArgumentException("ideal_candidate_fit 형식이 올바르지 않습니다: " + idealCandidateFitObj.getClass());
            }
            
            if (jobDescriptionFitObj instanceof Integer) {
                jobDescriptionFit = (Integer) jobDescriptionFitObj;
            } else if (jobDescriptionFitObj instanceof String) {
                jobDescriptionFit = Integer.parseInt(((String) jobDescriptionFitObj).trim());
            } else if (jobDescriptionFitObj instanceof Number) {
                jobDescriptionFit = ((Number) jobDescriptionFitObj).intValue();
            } else {
                throw new IllegalArgumentException("job_description_fit 형식이 올바르지 않습니다: " + jobDescriptionFitObj.getClass());
            }

            InterviewScore score = InterviewScore.create();
            score.setSession(session);
            score.setOverallScore(scoreDecimal);
            score.setOverallComment((String) finalReport.get("overall_comment"));
            score.setStrengths((String) finalReport.get("strengths"));
            score.setWeaknesses((String) finalReport.get("weaknesses"));
            score.setIdealCandidateFit(idealCandidateFit);
            score.setJobDescriptionFit(jobDescriptionFit);
            score.setJobPostingId(jobPosting.getId());
            score.setApplicantEmail(session.getApplicantEmail());
            score.setApplicantName(resume.getName());

            interviewScoreRepository.save(score);
            log.info("[Success] 채점 결과 저장 완료: Session={}, Score={}", session.getId(), scoreDecimal);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[Error] 채점 결과 저장 실패: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(new RuntimeException("채점 결과 저장 오류", e));
        }
    }
}

