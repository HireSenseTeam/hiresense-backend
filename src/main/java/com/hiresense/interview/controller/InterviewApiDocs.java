package com.hiresense.interview.controller;

import com.hiresense.global.error.ErrorResponse;
import com.hiresense.interview.dto.request.InterviewAnswerRequest;
import com.hiresense.interview.dto.request.InterviewStartRequest;
import com.hiresense.interview.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Interview", description = "면접 API")
public interface InterviewApiDocs {

    @Operation(summary = "면접 시작", description = "새로운 면접 세션을 시작하고 첫 번째 질문을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "면접 시작 성공",
                    content = @Content(schema = @Schema(implementation = InterviewStartResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 진행 중인 면접 또는 질문 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채용공고 또는 이력서를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InterviewStartResponse> startInterview(@RequestBody InterviewStartRequest request);

    @Operation(summary = "면접 답변 제출", description = "면접 질문에 대한 답변을 제출하고 다음 질문을 받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "답변 제출 성공",
                    content = @Content(schema = @Schema(implementation = InterviewAnswerResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "현재 면접 상태에서 답변 제출 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InterviewAnswerResponse> handleAnswer(@RequestBody InterviewAnswerRequest request);



    @Operation(summary = "면접 점수 조회", description = "면접 세션의 채점 결과를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "점수 조회 성공",
                    content = @Content(schema = @Schema(implementation = InterviewScoreResponse.class))),
            @ApiResponse(responseCode = "202", description = "채점이 아직 진행 중입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "채점 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "면접 세션 또는 점수를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))

    })
    ResponseEntity<InterviewScoreResponse> getScore(@RequestParam String sessionId);

    @Operation(summary = "면접 세션 조회", description = "면접 세션의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "면접 세션 조회 성공",
                    content = @Content(schema = @Schema(implementation = InterviewSessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InterviewSessionResponse> getSession(@PathVariable String sessionId);

    @Operation(summary = "면접 답변 목록 조회", description = "면접 세션의 모든 답변을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "답변 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<InterviewAnswerDetailResponse>> getAnswers(@PathVariable String sessionId);

    @Operation(summary = "면접 세션 목록 조회", description = "지원자 이메일 또는 채용공고 ID로 면접 세션 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "면접 세션 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (applicantEmail 또는 jobPostingId 중 하나는 필수)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<InterviewSessionResponse>> getSessions(
            @RequestParam(required = false) String applicantEmail,
            @RequestParam(required = false) Long jobPostingId);

    @Operation(summary = "면접 세션 삭제", description = "면접 세션과 관련된 모든 데이터를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "면접 세션 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteSession(@PathVariable String sessionId);
}
