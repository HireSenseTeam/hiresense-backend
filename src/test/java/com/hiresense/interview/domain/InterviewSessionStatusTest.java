package com.hiresense.interview.domain;

import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import com.hiresense.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewSessionStatusTest {

    @Test
    @DisplayName("면접 종료 후 채점 중, 채점 완료 상태로 전환된다")
    void scoringLifecycle() {
        InterviewSession session = createSession();

        session.complete();
        session.startScoring();
        session.markAsScored();

        assertThat(session.getStatus()).isEqualTo(InterviewStatus.SCORED);
    }

    @Test
    @DisplayName("진행 중인 면접은 바로 채점 중 상태로 전환할 수 없다")
    void cannotStartScoringBeforeComplete() {
        InterviewSession session = createSession();

        assertThatThrownBy(session::startScoring)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("채점 중인 면접은 실패 상태로 전환될 수 있다")
    void scoringFailureLifecycle() {
        InterviewSession session = createSession();

        session.complete();
        session.startScoring();
        session.markScoringFailed();

        assertThat(session.getStatus()).isEqualTo(InterviewStatus.SCORING_FAILED);
    }

    private InterviewSession createSession() {
        User company = User.createCompany("company@test.com", "password", "테스트기업");
        JobPosting jobPosting = JobPosting.builder()
                .companyName("테스트기업")
                .jobTitle("백엔드 개발자")
                .workLocation("서울")
                .recruitmentPeriod("2025-01-01 ~ 2025-12-31")
                .qualifications("Java, Spring")
                .idealCandidate("성실한 개발자")
                .preferredQualifications("AWS 경험")
                .jobDescription("백엔드 개발")
                .user(company)
                .build();
        Resume resume = Resume.builder()
                .name("지원자")
                .email("applicant@test.com")
                .build();

        return InterviewSession.create(jobPosting, resume, resume.getEmail());
    }
}
