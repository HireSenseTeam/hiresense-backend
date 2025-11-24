package com.hiresense.jobPosting.controller;

import com.hiresense.global.error.ErrorResponse;
import com.hiresense.jobPosting.dto.request.JobPostingRequest;
import com.hiresense.jobPosting.dto.request.JobPostingUpdateRequest;
import com.hiresense.jobPosting.dto.response.JobPostingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "JobPosting", description = "채용 공고 API")
public interface JobPostingApiDocs {

    @Operation(summary = "채용 공고 생성", description = "채용 공고를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "채용 공고 생성 성공",
                    content = @Content(schema = @Schema(implementation = JobPostingResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<JobPostingResponse> createJobPosting(@RequestBody JobPostingRequest request);

    @Operation(summary = "채용 공고 전체 조회", description = "모든 채용 공고를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채용 공고 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class)))
    })
    ResponseEntity<List<JobPostingResponse>> getAllJobPostings();

    @Operation(summary = "채용 공고 단건 조회", description = "채용 공고 한 건을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채용 공고 조회 성공",
                    content = @Content(schema = @Schema(implementation = JobPostingResponse.class))),
            @ApiResponse(responseCode = "404", description = "채용 공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<JobPostingResponse> getJobPostingById(@PathVariable Long id);

    @Operation(summary = "채용 공고 수정", description = "채용 공고의 일부 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "채용 공고 수정 성공"),
            @ApiResponse(responseCode = "404", description = "채용 공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> updateJobPosting(@PathVariable Long id, @RequestBody JobPostingUpdateRequest request);

    @Operation(summary = "채용 공고 삭제", description = "채용 공고를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "채용 공고 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "채용 공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteJobPosting(@PathVariable Long id);

    @Operation(summary = "채용 공고 질문 조회", description = "특정 채용 공고에 대한 면접 질문 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "채용 공고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<com.hiresense.question.dto.response.QuestionResponse>> getQuestions(@PathVariable Long id);
}
