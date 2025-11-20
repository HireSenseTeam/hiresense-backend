package com.hiresense.question.repository;

import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByType(QuestionType type);
    List<Question> findByJobPostingId(Long jobPostingId);
    List<Question> findByResumeId(Long resumeId);
}
