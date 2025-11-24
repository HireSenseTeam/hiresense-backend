package com.hiresense.interview.dto.response;

public record InterviewAnswerResponse(
        String question,
        String message
) {
    public static InterviewAnswerResponse withQuestion(String question) {
        return new InterviewAnswerResponse(question, null);
    }

    public static InterviewAnswerResponse withMessage(String message) {
        return new InterviewAnswerResponse(null, message);
    }
}
