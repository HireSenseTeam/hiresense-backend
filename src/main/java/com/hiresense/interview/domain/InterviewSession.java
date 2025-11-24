package com.hiresense.interview.domain;

import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession extends BaseTimeEntity {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false)
    private String applicantEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(nullable = false)
    private Integer currentIndex = 0;

    public static InterviewSession create(JobPosting jobPosting, Resume resume, String applicantEmail) {
        InterviewSession session = new InterviewSession();
        session.id = UUID.randomUUID().toString();
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

    public void markAsScored() {
        if (this.status != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("면접이 완료되지 않아 채점할 수 없습니다. 현재 상태: " + this.status);
        }
        this.status = InterviewStatus.SCORED;
    }

    public void markAsError() {
        this.status = InterviewStatus.ERROR;
    }
}
