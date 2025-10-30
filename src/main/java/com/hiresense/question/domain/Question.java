package com.hiresense.question.domain;

import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    // JOB_POSTING 질문일 경우, 어떤 공고에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    // RESUME 질문일 경우, 어떤 이력서에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Builder
    public Question(String text, QuestionType type) {
        this.text = text;
        this.type = type;
    }
}
