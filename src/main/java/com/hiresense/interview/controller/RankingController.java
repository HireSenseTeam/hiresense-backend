package com.hiresense.interview.controller;

import com.hiresense.interview.dto.response.RankingResponse;
import com.hiresense.interview.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/{jobId}")
    public ResponseEntity<List<RankingResponse>> getRankings(@PathVariable Long jobId) {
        List<RankingResponse> rankings = rankingService.getRankings(jobId);
        return ResponseEntity.ok(rankings);
    }
}

