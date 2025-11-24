package com.hiresense.interview.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InterviewAnswerDetailResponse(
        Long answerId,
        Long questionId,
        String questionText,
        String answerText,
        BigDecimal score,
        String feedback,
        LocalDateTime createdAt
) {
}
