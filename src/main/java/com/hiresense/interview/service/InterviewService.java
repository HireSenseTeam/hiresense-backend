package com.hiresense.interview.service;

import com.hiresense.ai.service.BedrockService;
import com.hiresense.ai.service.InterviewScoringService;
import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;
import com.hiresense.interview.domain.InterviewAnswer;
import com.hiresense.interview.domain.InterviewQuestion;
import com.hiresense.interview.domain.InterviewScore;
import com.hiresense.interview.domain.InterviewSession;
import com.hiresense.interview.domain.InterviewStatus;
import com.hiresense.interview.dto.request.InterviewAnswerRequest;
import com.hiresense.interview.dto.request.InterviewStartRequest;
import com.hiresense.interview.dto.response.InterviewAnswerDetailResponse;
import com.hiresense.interview.dto.response.InterviewAnswerResponse;
import com.hiresense.interview.dto.response.InterviewScoreResponse;
import com.hiresense.interview.dto.response.InterviewSessionResponse;
import com.hiresense.interview.dto.response.InterviewStartResponse;
import com.hiresense.interview.repository.InterviewAnswerRepository;
import com.hiresense.interview.repository.InterviewQuestionRepository;
import com.hiresense.interview.repository.InterviewSessionRepository;
import com.hiresense.interview.repository.InterviewScoreRepository;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.jobPosting.repository.JobPostingRepository;
import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.repository.QuestionRepository;
import com.hiresense.resume.domain.Resume;
import com.hiresense.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ResumeRepository resumeRepository;
    private final QuestionRepository questionRepository;
    private final BedrockService bedrockService;
    private final InterviewScoringService interviewScoringService;

    @Transactional
    public InterviewStartResponse startInterview(InterviewStartRequest request) {
        log.info("면접 시작 요청: jobId={}, applicantEmail={}", request.jobId(), request.applicantEmail());

        JobPosting jobPosting = jobPostingRepository.findById(request.jobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));

        log.info("이력서 조회 시도: applicantEmail={}", request.applicantEmail());
        Resume resume = resumeRepository.findByEmail(request.applicantEmail())
                .orElseThrow(() -> {
                    log.error("이력서를 찾을 수 없습니다: applicantEmail={}", request.applicantEmail());
                    return new BusinessException(ErrorCode.RESUME_NOT_FOUND);
                });
        log.info("이력서 조회 성공: resumeId={}, email={}", resume.getId(), resume.getEmail());

        validateNoActiveInterview(request.jobId(), request.applicantEmail());

        List<Question> allQuestions = loadInterviewQuestions(jobPosting.getId(), resume.getId());
        if (allQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_NO_QUESTIONS);
        }

        InterviewSession session = InterviewSession.create(jobPosting, resume, request.applicantEmail());
        session = interviewSessionRepository.save(session);

        List<InterviewQuestion> interviewQuestions = new ArrayList<>();
        for (int i = 0; i < allQuestions.size(); i++) {
            InterviewQuestion interviewQuestion = InterviewQuestion.create(session, allQuestions.get(i), i);
            interviewQuestions.add(interviewQuestion);
        }
        interviewQuestionRepository.saveAll(interviewQuestions);

        String firstQuestion = allQuestions.get(0).getText();

        log.info("면접 세션 생성 완료: sessionId={}, 총 질문 수={}", session.getId(), allQuestions.size());
        return new InterviewStartResponse(session.getId(), firstQuestion);
    }

    @Transactional
    public InterviewAnswerResponse handleAnswer(InterviewAnswerRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("답변 처리 요청: sessionId={}", request.sessionId());

        InterviewSession session = getSessionOrThrow(request.sessionId());

        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(
                    ErrorCode.INTERVIEW_INVALID_STATUS,
                    "진행 중인 면접만 답변을 제출할 수 있습니다. 현재 상태: " + session.getStatus()
            );
        }

        List<InterviewQuestion> interviewQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrder(session.getId());

        if (interviewQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_NO_QUESTIONS);
        }

        if (session.getCurrentIndex() >= interviewQuestions.size()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED);
        }

        InterviewQuestion currentInterviewQuestion = interviewQuestions.get(session.getCurrentIndex());
        Question currentQuestion = currentInterviewQuestion.getQuestion();

        InterviewAnswer answer = InterviewAnswer.create(session, currentQuestion, request.answerText());
        interviewAnswerRepository.save(answer);

        log.info("답변 저장 완료: questionId={}", currentQuestion.getId());

        session.incrementCurrentIndex();

        int nextIndex = session.getCurrentIndex();
        if (nextIndex >= interviewQuestions.size()) {
            session.complete();
            session.startScoring();
            interviewSessionRepository.save(session);
            log.info("면접 종료 및 채점 대기 상태 전환: sessionId={}", session.getId());

            scheduleAsyncScoringAfterCommit(
                    session.getId(),
                    session.getJobPosting().getId(),
                    session.getApplicantEmail()
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[응답 반환] 면접 종료 응답 시간: {}ms (채점은 백그라운드에서 계속 진행 중)", elapsed);
            return InterviewAnswerResponse.withMessage("면접이 종료되었습니다. 수고하셨습니다.");
        }

        InterviewQuestion nextInterviewQuestion = interviewQuestions.get(nextIndex);
        String nextQuestionText = nextInterviewQuestion.getQuestion().getText();

        String responseText = bedrockService.getTailBitingResponse(
                request.answerText(),
                nextQuestionText
        );

        return InterviewAnswerResponse.withQuestion(responseText);
    }

    public InterviewScoreResponse getScore(String sessionId) {
        log.info("점수 조회 요청: sessionId={}", sessionId);

        InterviewSession session = getSessionOrThrow(sessionId);

        InterviewScore score = interviewScoreRepository.findBySessionId(sessionId)
                .orElseThrow(() -> scoringNotReadyException(session));

        return new InterviewScoreResponse(
                score.getOverallScore(),
                score.getOverallComment(),
                score.getStrengths(),
                score.getWeaknesses(),
                score.getIdealCandidateFit(),
                score.getJobDescriptionFit()
        );
    }

    public InterviewSessionResponse getSession(String sessionId) {
        log.info("면접 세션 조회 요청: sessionId={}", sessionId);

        InterviewSession session = getSessionOrThrow(sessionId);

        int totalQuestions = interviewQuestionRepository.countByInterviewSessionId(sessionId);

        return toInterviewSessionResponse(session, totalQuestions);
    }

    public List<InterviewAnswerDetailResponse> getAnswers(String sessionId) {
        log.info("면접 답변 조회 요청: sessionId={}", sessionId);

        getSessionOrThrow(sessionId);

        List<InterviewAnswer> answers = interviewAnswerRepository.findBySessionId(sessionId);

        return answers.stream()
                .map(answer -> new InterviewAnswerDetailResponse(
                        answer.getId(),
                        answer.getQuestion().getId(),
                        answer.getQuestion().getText(),
                        answer.getAnswerText(),
                        answer.getScore(),
                        answer.getFeedback(),
                        answer.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<InterviewSessionResponse> getSessionsByApplicant(String applicantEmail) {
        log.info("지원자별 면접 세션 조회: applicantEmail={}", applicantEmail);

        List<InterviewSession> sessions = interviewSessionRepository.findByApplicantEmail(applicantEmail);
        Map<String, Integer> questionCounts = getQuestionCounts(sessions);

        return sessions.stream()
                .map(session -> toInterviewSessionResponse(session, questionCounts.getOrDefault(session.getId(), 0)))
                .collect(Collectors.toList());
    }

    public List<InterviewSessionResponse> getSessionsByJobPosting(Long jobPostingId) {
        log.info("채용공고별 면접 세션 조회: jobPostingId={}", jobPostingId);

        List<InterviewSession> sessions = interviewSessionRepository.findByJobPostingId(jobPostingId);
        Map<String, Integer> questionCounts = getQuestionCounts(sessions);

        return sessions.stream()
                .map(session -> toInterviewSessionResponse(session, questionCounts.getOrDefault(session.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(String sessionId) {
        log.info("면접 세션 삭제 요청: sessionId={}", sessionId);

        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        List<InterviewAnswer> answers = interviewAnswerRepository.findBySessionId(sessionId);
        if (!answers.isEmpty()) {
            interviewAnswerRepository.deleteAll(answers);
        }

        List<InterviewQuestion> questions = interviewQuestionRepository.findByInterviewSessionIdOrderByQuestionOrder(sessionId);
        if (!questions.isEmpty()) {
            interviewQuestionRepository.deleteAll(questions);
        }

        interviewScoreRepository.findBySessionId(sessionId).ifPresent(interviewScoreRepository::delete);

        interviewSessionRepository.delete(session);

        log.info("면접 세션 삭제 완료: sessionId={}", sessionId);
    }

    private void validateNoActiveInterview(Long jobPostingId, String applicantEmail) {
        List<InterviewStatus> activeStatuses = List.of(InterviewStatus.IN_PROGRESS, InterviewStatus.SCORING);

        interviewSessionRepository.findFirstByJobPostingIdAndApplicantEmailAndStatusIn(
                jobPostingId,
                applicantEmail,
                activeStatuses
        ).ifPresent(session -> {
            throw new BusinessException(
                    ErrorCode.INTERVIEW_ALREADY_IN_PROGRESS,
                    "이미 진행 중이거나 채점 중인 면접이 있습니다. sessionId: " + session.getId()
            );
        });
    }

    private List<Question> loadInterviewQuestions(Long jobPostingId, Long resumeId) {
        List<Question> allQuestions = new ArrayList<>();

        List<Question> commonQuestions = questionRepository.findByType(QuestionType.COMMON);
        allQuestions.addAll(commonQuestions);
        log.info("공통 질문 {}개 로드", commonQuestions.size());

        List<Question> resumeQuestions = questionRepository.findByResumeId(resumeId);
        allQuestions.addAll(resumeQuestions);
        log.info("이력서 질문 {}개 로드", resumeQuestions.size());

        List<Question> jobPostingQuestions = questionRepository.findByJobPostingId(jobPostingId);
        allQuestions.addAll(jobPostingQuestions);
        log.info("채용 공고 질문 {}개 로드", jobPostingQuestions.size());

        return allQuestions;
    }

    private void scheduleAsyncScoringAfterCommit(String sessionId, Long jobPostingId, String applicantEmail) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            startAsyncScoring(sessionId, jobPostingId, applicantEmail);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                startAsyncScoring(sessionId, jobPostingId, applicantEmail);
            }
        });
    }

    private void startAsyncScoring(String sessionId, Long jobPostingId, String applicantEmail) {
        log.info("[비동기 채점 시작] ID만 전달해 백그라운드 스레드에서 재조회합니다. sessionId={}", sessionId);
        interviewScoringService.scoreInterview(sessionId, jobPostingId, applicantEmail)
                .thenRun(() -> markScoringSucceeded(sessionId))
                .exceptionally(ex -> {
                    log.error("면접 채점 중 오류 발생: sessionId={}, error={}", sessionId, ex.getMessage(), ex);
                    markScoringFailed(sessionId);
                    return null;
                });
    }

    private void markScoringSucceeded(String sessionId) {
        interviewSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() != InterviewStatus.SCORING) {
                log.warn("채점 완료 상태 전환을 건너뜁니다. sessionId={}, status={}", sessionId, session.getStatus());
                return;
            }
            session.markAsScored();
            interviewSessionRepository.save(session);
            log.info("면접 채점 완료: sessionId={}", sessionId);
        });
    }

    private void markScoringFailed(String sessionId) {
        interviewSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() != InterviewStatus.SCORING) {
                log.warn("채점 실패 상태 전환을 건너뜁니다. sessionId={}, status={}", sessionId, session.getStatus());
                return;
            }
            session.markScoringFailed();
            interviewSessionRepository.save(session);
            log.info("면접 채점 실패 상태 저장: sessionId={}", sessionId);
        });
    }

    private BusinessException scoringNotReadyException(InterviewSession session) {
        if (session.getStatus() == InterviewStatus.SCORING_FAILED) {
            return new BusinessException(ErrorCode.SCORING_FAILED);
        }
        if (session.getStatus() == InterviewStatus.SCORED) {
            return new BusinessException(ErrorCode.SCORING_FAILED, "채점 완료 상태이지만 점수 데이터가 없습니다.");
        }
        return new BusinessException(ErrorCode.SCORING_IN_PROGRESS);
    }

    private InterviewSession getSessionOrThrow(String sessionId) {
        return interviewSessionRepository.findWithDetailsById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
    }

    private Map<String, Integer> getQuestionCounts(List<InterviewSession> sessions) {
        if (sessions.isEmpty()) {
            return Map.of();
        }

        List<String> sessionIds = sessions.stream()
                .map(InterviewSession::getId)
                .collect(Collectors.toList());

        return interviewQuestionRepository.countByInterviewSessionIds(sessionIds).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    private InterviewSessionResponse toInterviewSessionResponse(InterviewSession session, int totalQuestions) {
        return new InterviewSessionResponse(
                session.getId(),
                session.getJobPosting().getId(),
                session.getResume().getId(),
                session.getApplicantEmail(),
                session.getStatus(),
                session.getCurrentIndex(),
                totalQuestions,
                session.getCreatedAt(),
                session.getModifiedAt()
        );
    }
}
