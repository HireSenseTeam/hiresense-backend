package com.hiresense.interview.repository;

import com.hiresense.interview.domain.InterviewScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewScoreRepository extends JpaRepository<InterviewScore, Long> {
    List<InterviewScore> findByJobPostingIdOrderByOverallScoreDesc(Long jobPostingId);
    
    @Query("SELECT s FROM InterviewScore s WHERE s.session.id = :sessionId")
    Optional<InterviewScore> findBySessionId(@Param("sessionId") String sessionId);
}
