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
        if (this.status != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 면접만 완료할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InterviewStatus.COMPLETED;
    }

    public void startScoring() {
        if (this.status != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("완료된 면접만 채점을 시작할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InterviewStatus.SCORING;
    }

    public void markAsScored() {
        if (this.status != InterviewStatus.SCORING) {
            throw new IllegalStateException("채점 중인 면접만 채점 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InterviewStatus.SCORED;
    }

    public void markScoringFailed() {
        if (this.status != InterviewStatus.SCORING) {
            throw new IllegalStateException("채점 중인 면접만 채점 실패 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InterviewStatus.SCORING_FAILED;
    }
}
