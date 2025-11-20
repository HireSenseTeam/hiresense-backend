package com.hiresense.interview.service;

import com.hiresense.interview.dto.response.RankingResponse;
import com.hiresense.interview.repository.InterviewScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final InterviewScoreRepository interviewScoreRepository;

    public List<RankingResponse> getRankings(Long jobId) {
        log.info("랭킹 조회 요청: jobId={}", jobId);

        List<com.hiresense.interview.domain.InterviewScore> scores = 
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobId);

        log.info("DynamoDB 조회 결과: {}개 항목", scores.size());

        List<RankingResponse> rankings = IntStream.range(0, scores.size())
                .mapToObj(i -> {
                    com.hiresense.interview.domain.InterviewScore score = scores.get(i);
                    return new RankingResponse(
                            score.getApplicantEmail(),
                            score.getOverallScore(),
                            score.getApplicantName(),
                            i + 1
                    );
                })
                .toList();

        log.info("정렬된 랭킹: {}개", rankings.size());
        return rankings;
    }
}

