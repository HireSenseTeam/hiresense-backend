package com.hiresense.question.controller;

import com.hiresense.global.error.ErrorResponse;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.dto.response.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Question", description = "질문 API")
public interface QuestionApiDocs {

    @Operation(summary = "타입별 질문 조회", description = "질문 타입(COMMON, JOB_POSTING, RESUME)별로 질문 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<QuestionResponse>> getQuestionsByType(@PathVariable QuestionType type);

    @Operation(summary = "채용공고별 질문 조회", description = "특정 채용공고에 연결된 질문 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "채용공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<QuestionResponse>> getQuestionsByJobPosting(@PathVariable Long jobPostingId);

    @Operation(summary = "이력서별 질문 조회", description = "특정 이력서에 연결된 질문 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<QuestionResponse>> getQuestionsByResume(@PathVariable Long resumeId);
}
