package com.hiresense.interview.service;

import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;
import com.hiresense.interview.domain.InterviewScore;
import com.hiresense.interview.dto.response.RankingResponse;
import com.hiresense.interview.repository.InterviewScoreRepository;
import com.hiresense.jobPosting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final InterviewScoreRepository interviewScoreRepository;
    private final JobPostingRepository jobPostingRepository;

    public List<RankingResponse> getRankings(Long jobId) {
        log.info("랭킹 조회 요청: jobId={}", jobId);

        if (!jobPostingRepository.existsById(jobId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND);
        }

        List<InterviewScore> interviewScores = 
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobId);

        log.info("면접 점수 조회 결과: {}개 항목", interviewScores.size());

        List<RankingResponse> rankings = calculateRankings(interviewScores);

        log.info("정렬된 랭킹: {}개", rankings.size());
        return rankings;
    }

    private List<RankingResponse> calculateRankings(List<InterviewScore> interviewScores) {
        if (interviewScores.isEmpty()) {
            return new ArrayList<>();
        }

        List<RankingResponse> rankings = new ArrayList<>();
        int currentRank = 1;
        BigDecimal previousScore = null;

        for (int i = 0; i < interviewScores.size(); i++) {
            InterviewScore score = interviewScores.get(i);
            BigDecimal currentScore = score.getOverallScore();

            if (previousScore != null && currentScore.compareTo(previousScore) != 0) {
                currentRank = i + 1;
            }

            rankings.add(new RankingResponse(
                    score.getApplicantEmail(),
                    score.getOverallScore(),
                    score.getApplicantName(),
                    currentRank
            ));

            previousScore = currentScore;
        }

        return rankings;
    }
}
