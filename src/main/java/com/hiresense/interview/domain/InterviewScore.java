package com.hiresense.interview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", unique = true, nullable = false)
    private InterviewSession session;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;
    
    @Column(length = 2000)
    private String overallComment;
    
    @Column(length = 1000)
    private String strengths;
    
    @Column(length = 1000)
    private String weaknesses;
    
    @Column(nullable = false)
    private int idealCandidateFit;
    
    @Column(nullable = false)
    private int jobDescriptionFit;
    
    @Column(nullable = false)
    private Long jobPostingId;
    
    @Column(nullable = false)
    private String applicantEmail;
    
    private String applicantName;

    public static InterviewScore create(
            InterviewSession session,
            BigDecimal overallScore,
            String overallComment,
            String strengths,
            String weaknesses,
            int idealCandidateFit,
            int jobDescriptionFit,
            Long jobPostingId,
            String applicantEmail,
            String applicantName) {
        InterviewScore score = new InterviewScore();
        score.session = session;
        score.overallScore = overallScore;
        score.overallComment = overallComment;
        score.strengths = strengths;
        score.weaknesses = weaknesses;
        score.idealCandidateFit = idealCandidateFit;
        score.jobDescriptionFit = jobDescriptionFit;
        score.jobPostingId = jobPostingId;
        score.applicantEmail = applicantEmail;
        score.applicantName = applicantName;
        return score;
    }
}
