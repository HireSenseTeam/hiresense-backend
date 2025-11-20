package com.hiresense.interview.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InterviewAnswerRequest(
        @NotBlank(message = "session_id는 필수입니다.")
        String sessionId,
        
        @NotBlank(message = "answer_text는 필수입니다.")
        String answerText
) {
}

