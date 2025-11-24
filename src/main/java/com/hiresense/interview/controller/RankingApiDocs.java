package com.hiresense.interview.controller;

import com.hiresense.global.error.ErrorResponse;
import com.hiresense.interview.dto.response.RankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Ranking", description = "지원자 랭킹 API")
public interface RankingApiDocs {

    @Operation(summary = "채용공고별 지원자 랭킹 조회", 
               description = "특정 채용공고에 대한 지원자들의 면접 점수 순위를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "랭킹 조회 성공",
                    content = @Content(schema = @Schema(implementation = RankingResponse.class))),
            @ApiResponse(responseCode = "404", description = "채용공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<RankingResponse>> getRankings(@PathVariable Long jobId);
}
