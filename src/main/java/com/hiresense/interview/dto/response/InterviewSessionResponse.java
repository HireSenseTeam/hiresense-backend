package com.hiresense.interview.dto.response;

import com.hiresense.interview.domain.InterviewStatus;

import java.time.LocalDateTime;

public record InterviewSessionResponse(
        String sessionId,
        Long jobPostingId,
        Long resumeId,
        String applicantEmail,
        InterviewStatus status,
        Integer currentIndex,
        Integer totalQuestions,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
}

