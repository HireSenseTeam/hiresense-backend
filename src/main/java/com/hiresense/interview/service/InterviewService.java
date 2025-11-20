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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        // JobPosting 조회
        JobPosting jobPosting = jobPostingRepository.findById(request.jobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));

        // Resume 조회 (이메일로)
        log.info("이력서 조회 시도: applicantEmail={}", request.applicantEmail());
        Resume resume = resumeRepository.findByEmail(request.applicantEmail())
                .orElseThrow(() -> {
                    log.error("이력서를 찾을 수 없습니다: applicantEmail={}", request.applicantEmail());
                    return new BusinessException(ErrorCode.RESUME_NOT_FOUND);
                });
        log.info("이력서 조회 성공: resumeId={}, email={}", resume.getId(), resume.getEmail());

        // 중복 면접 방지: 같은 지원자가 같은 공고에 진행 중인 면접이 있는지 확인
        Optional<InterviewSession> existingSession = interviewSessionRepository
                .findByJobPostingIdAndApplicantEmailAndStatus(
                        request.jobId(), 
                        request.applicantEmail(), 
                        InterviewStatus.IN_PROGRESS
                );
        if (existingSession.isPresent()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "이미 진행 중인 면접이 있습니다. sessionId: " + existingSession.get().getId());
        }

        // 질문 수집
        List<Question> allQuestions = new ArrayList<>();

        // 1. 공통 질문 (COMMON 타입)
        List<Question> commonQuestions = questionRepository.findByType(QuestionType.COMMON);
        allQuestions.addAll(commonQuestions);
        log.info("공통 질문 {}개 로드", commonQuestions.size());

        // 2. 이력서 기반 질문 (RESUME 타입)
        List<Question> resumeQuestions = questionRepository.findByResumeId(resume.getId());
        allQuestions.addAll(resumeQuestions);
        log.info("이력서 질문 {}개 로드", resumeQuestions.size());

        // 3. 채용 공고 기반 질문 (JOB_POSTING 타입)
        List<Question> jobPostingQuestions = questionRepository.findByJobPostingId(jobPosting.getId());
        allQuestions.addAll(jobPostingQuestions);
        log.info("채용 공고 질문 {}개 로드", jobPostingQuestions.size());

        if (allQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "질문이 없습니다.");
        }

        // InterviewSession 생성
        InterviewSession session = InterviewSession.create(jobPosting, resume, request.applicantEmail());
        session = interviewSessionRepository.save(session);

        // InterviewQuestion 생성 및 저장
        List<InterviewQuestion> interviewQuestions = new ArrayList<>();
        for (int i = 0; i < allQuestions.size(); i++) {
            InterviewQuestion interviewQuestion = InterviewQuestion.create(session, allQuestions.get(i), i);
            interviewQuestions.add(interviewQuestion);
        }
        interviewQuestionRepository.saveAll(interviewQuestions);

        // 첫 번째 질문 반환
        String firstQuestion = allQuestions.get(0).getText();

        log.info("면접 세션 생성 완료: sessionId={}, 총 질문 수={}", session.getId(), allQuestions.size());
        return new InterviewStartResponse(session.getId(), firstQuestion);
    }

    @Transactional
    public InterviewAnswerResponse handleAnswer(InterviewAnswerRequest request) {
        log.info("답변 처리 요청: sessionId={}", request.sessionId());

        InterviewSession session = interviewSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 session_id입니다."));

        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "진행 중인 면접이 아닙니다.");
        }

        // 현재 질문 조회
        List<InterviewQuestion> interviewQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrder(session.getId());
        
        if (session.getCurrentIndex() >= interviewQuestions.size()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 모든 질문에 답변했습니다.");
        }

        InterviewQuestion currentInterviewQuestion = interviewQuestions.get(session.getCurrentIndex());
        Question currentQuestion = currentInterviewQuestion.getQuestion();

        // 답변 저장
        InterviewAnswer answer = InterviewAnswer.create(session, currentQuestion, request.answerText());
        interviewAnswerRepository.save(answer);

        log.info("답변 저장 완료: questionId={}", currentQuestion.getId());

        // 다음 질문 인덱스로 이동
        session.incrementCurrentIndex();

        // 다음 질문이 있는지 확인
        int nextIndex = session.getCurrentIndex();
        if (nextIndex >= interviewQuestions.size()) {
            // 면접 종료
            session.complete();
            interviewSessionRepository.save(session);
            log.info("면접 종료: sessionId={}", session.getId());
            
            // 비동기로 채점 시작 (백그라운드 처리)
            // JobPosting ID를 전달하여 LazyInitializationException 방지
            interviewScoringService.scoreInterview(session, session.getJobPosting().getId())
                    .thenRun(() -> {
                        // 채점 완료 후 상태를 SCORED로 업데이트
                        InterviewSession updatedSession = interviewSessionRepository.findById(session.getId())
                                .orElse(null);
                        if (updatedSession != null) {
                            updatedSession.setStatus(InterviewStatus.SCORED);
                            interviewSessionRepository.save(updatedSession);
                            log.info("면접 채점 완료: sessionId={}", session.getId());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("면접 채점 중 오류 발생: sessionId={}, error={}", session.getId(), ex.getMessage(), ex);
                        // 채점 실패 시 ERROR 상태로 변경
                        InterviewSession updatedSession = interviewSessionRepository.findById(session.getId())
                                .orElse(null);
                        if (updatedSession != null) {
                            updatedSession.setStatus(InterviewStatus.ERROR);
                            interviewSessionRepository.save(updatedSession);
                        }
                        return null;
                    });
            
            return InterviewAnswerResponse.withMessage("면접이 종료되었습니다. 수고하셨습니다.");
        }

        // 다음 질문 가져오기
        InterviewQuestion nextInterviewQuestion = interviewQuestions.get(nextIndex);
        String nextQuestionText = nextInterviewQuestion.getQuestion().getText();

        // 꼬리물기 응답 생성
        String responseText = bedrockService.getTailBitingResponse(
                request.answerText(),
                nextQuestionText
        );

        interviewSessionRepository.save(session);
        return InterviewAnswerResponse.withQuestion(responseText);
    }

    public InterviewScoreResponse getScore(String sessionId) {
        log.info("점수 조회 요청: sessionId={}", sessionId);

        // 세션 존재 여부 확인
        if (!interviewSessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 session_id입니다.");
        }

        InterviewScore score = interviewScoreRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCORING_IN_PROGRESS));

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

        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 session_id입니다."));

        // 총 질문 수 계산
        List<InterviewQuestion> interviewQuestions = interviewQuestionRepository
                .findByInterviewSessionIdOrderByQuestionOrder(sessionId);
        int totalQuestions = interviewQuestions.size();

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

    public List<InterviewAnswerDetailResponse> getAnswers(String sessionId) {
        log.info("면접 답변 조회 요청: sessionId={}", sessionId);

        // 세션 존재 여부 확인
        if (!interviewSessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 session_id입니다.");
        }

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

        return sessions.stream()
                .map(session -> {
                    List<InterviewQuestion> questions = interviewQuestionRepository
                            .findByInterviewSessionIdOrderByQuestionOrder(session.getId());
                    return new InterviewSessionResponse(
                            session.getId(),
                            session.getJobPosting().getId(),
                            session.getResume().getId(),
                            session.getApplicantEmail(),
                            session.getStatus(),
                            session.getCurrentIndex(),
                            questions.size(),
                            session.getCreatedAt(),
                            session.getModifiedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<InterviewSessionResponse> getSessionsByJobPosting(Long jobPostingId) {
        log.info("채용공고별 면접 세션 조회: jobPostingId={}", jobPostingId);

        List<InterviewSession> sessions = interviewSessionRepository.findByJobPostingId(jobPostingId);

        return sessions.stream()
                .map(session -> {
                    List<InterviewQuestion> questions = interviewQuestionRepository
                            .findByInterviewSessionIdOrderByQuestionOrder(session.getId());
                    return new InterviewSessionResponse(
                            session.getId(),
                            session.getJobPosting().getId(),
                            session.getResume().getId(),
                            session.getApplicantEmail(),
                            session.getStatus(),
                            session.getCurrentIndex(),
                            questions.size(),
                            session.getCreatedAt(),
                            session.getModifiedAt()
                    );
                })
                .collect(Collectors.toList());
    }

}

