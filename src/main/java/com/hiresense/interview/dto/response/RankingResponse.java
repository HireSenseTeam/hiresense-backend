package com.hiresense.interview.dto.response;

import java.math.BigDecimal;

public record RankingResponse(
        String applicantEmail,
        BigDecimal overallScore,
        String applicantName,
        Integer rank
) {
}

