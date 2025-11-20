package com.hiresense.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewStartRequest(
        @NotNull(message = "job_id는 필수입니다.")
        Long jobId,
        
        @NotBlank(message = "applicantEmail은 필수입니다.")
        String applicantEmail
) {
}

