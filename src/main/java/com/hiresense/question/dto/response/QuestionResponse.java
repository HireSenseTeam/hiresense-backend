package com.hiresense.question.dto.response;

import com.hiresense.question.domain.QuestionType;

public record QuestionResponse(
        Long id,
        String text,
        QuestionType type,
        Long jobPostingId,
        Long resumeId
) {
}

