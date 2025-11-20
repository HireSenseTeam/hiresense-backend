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
    @JoinColumn(name = "session_id", unique = true)
    private InterviewSession session;
    
    private BigDecimal overallScore;
    private String overallComment;
    private String strengths;
    private String weaknesses;
    private int idealCandidateFit;
    private int jobDescriptionFit;
    
    // 랭킹 조회를 위한 필드 (중복 저장)
    @Column(nullable = false)
    private Long jobPostingId;
    
    @Column(nullable = false)
    private String applicantEmail;
    
    private String applicantName;

    public void setSession(InterviewSession session) {
        this.session = session;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public void setOverallComment(String overallComment) {
        this.overallComment = overallComment;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public void setIdealCandidateFit(int idealCandidateFit) {
        this.idealCandidateFit = idealCandidateFit;
    }

    public void setJobDescriptionFit(int jobDescriptionFit) {
        this.jobDescriptionFit = jobDescriptionFit;
    }

    public void setJobPostingId(Long jobPostingId) {
        this.jobPostingId = jobPostingId;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public static InterviewScore create() {
        return new InterviewScore();
    }
}
