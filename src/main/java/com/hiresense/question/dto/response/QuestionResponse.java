package com.hiresense.question.dto.response;

import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;

public record QuestionResponse(
        Long id,
        String text,
        QuestionType type,
        Long jobPostingId,
        Long resumeId
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getType(),
                question.getJobPosting() != null ? question.getJobPosting().getId() : null,
                question.getResume() != null ? question.getResume().getId() : null
        );
    }
}
