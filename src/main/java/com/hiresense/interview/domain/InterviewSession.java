package com.hiresense.interview.domain;

import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // session_id (UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(nullable = false)
    private String applicantEmail;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    @Column(nullable = false)
    private Integer currentIndex = 0;

    public static InterviewSession create(JobPosting jobPosting, Resume resume, String applicantEmail) {
        InterviewSession session = new InterviewSession();
        session.jobPosting = jobPosting;
        session.resume = resume;
        session.applicantEmail = applicantEmail;
        session.status = InterviewStatus.IN_PROGRESS;
        session.currentIndex = 0;
        return session;
    }

    public void incrementCurrentIndex() {
        this.currentIndex++;
    }

    public void complete() {
        this.status = InterviewStatus.COMPLETED;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }
}
