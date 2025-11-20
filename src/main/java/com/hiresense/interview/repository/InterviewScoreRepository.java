package com.hiresense.interview.repository;

import com.hiresense.interview.domain.InterviewScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewScoreRepository extends JpaRepository<InterviewScore, Long> {
    List<InterviewScore> findByJobPostingIdOrderByOverallScoreDesc(Long jobPostingId);
    Optional<InterviewScore> findBySessionId(String sessionId);
}

