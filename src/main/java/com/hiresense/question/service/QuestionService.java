package com.hiresense.question.service;

import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.dto.response.QuestionResponse;
import com.hiresense.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<QuestionResponse> findByType(QuestionType type) {
        log.info("질문 타입별 조회: type={}", type);
        List<Question> questions = questionRepository.findByType(type);
        return questions.stream()
                .map(QuestionResponse::from)
                .collect(Collectors.toList());
    }

    public List<QuestionResponse> findByJobPostingId(Long jobPostingId) {
        log.info("채용공고별 질문 조회: jobPostingId={}", jobPostingId);
        List<Question> questions = questionRepository.findByJobPostingId(jobPostingId);
        return questions.stream()
                .map(QuestionResponse::from)
                .collect(Collectors.toList());
    }

    public List<QuestionResponse> findByResumeId(Long resumeId) {
        log.info("이력서별 질문 조회: resumeId={}", resumeId);
        List<Question> questions = questionRepository.findByResumeId(resumeId);
        return questions.stream()
                .map(QuestionResponse::from)
                .collect(Collectors.toList());
    }
}
