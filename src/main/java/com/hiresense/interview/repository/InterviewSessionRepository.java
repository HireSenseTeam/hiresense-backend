package com.hiresense.interview.repository;

import com.hiresense.interview.domain.InterviewSession;
import com.hiresense.interview.domain.InterviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, String> {
    
    @EntityGraph(attributePaths = {"jobPosting", "resume"})
    List<InterviewSession> findByApplicantEmail(String applicantEmail);
    
    @EntityGraph(attributePaths = {"jobPosting", "resume"})
    List<InterviewSession> findByJobPostingId(Long jobPostingId);
    
        Optional<InterviewSession> findByJobPostingIdAndApplicantEmailAndStatus(Long jobPostingId, String applicantEmail, InterviewStatus status);
    
    @EntityGraph(attributePaths = {"jobPosting", "resume"})
    Optional<InterviewSession> findWithDetailsById(String id);
}
