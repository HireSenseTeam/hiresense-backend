package com.hiresense.interview.repository;

import com.hiresense.interview.domain.InterviewAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    @EntityGraph(attributePaths = "question")
    List<InterviewAnswer> findBySessionId(String sessionId);
}
