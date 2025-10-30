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
}
