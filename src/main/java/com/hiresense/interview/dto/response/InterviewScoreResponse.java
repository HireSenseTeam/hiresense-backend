package com.hiresense.interview.dto.response;

import java.math.BigDecimal;

public record InterviewScoreResponse(
        BigDecimal overallScore,
        String overallComment,
        String strengths,
        String weaknesses,
        Integer idealCandidateFit,
        Integer jobDescriptionFit
) {
}

