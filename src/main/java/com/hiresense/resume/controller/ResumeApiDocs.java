package com.hiresense.resume.controller;

import com.hiresense.global.error.ErrorResponse;
import com.hiresense.resume.dto.request.ResumeRequest;
import com.hiresense.resume.dto.request.ResumeUpdateRequest;
import com.hiresense.resume.dto.response.ResumeResponse;
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

@Tag(name = "Resume", description = "이력서 API")
public interface ResumeApiDocs {

    @Operation(summary = "이력서 생성", description = "이력서를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "이력서 생성 성공",
            content = @Content(schema = @Schema(implementation = ResumeResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ResumeResponse> createResume(@RequestBody ResumeRequest request);

    @Operation(summary = "이력서 수정", description = "이력서의 일부 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "이력서 수정 성공"),
        @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> updateResume(@PathVariable Long id, @RequestBody ResumeUpdateRequest request);

    @Operation(summary = "이력서 단건 조회", description = "이력서 한 건을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이력서 조회 성공",
            content = @Content(schema = @Schema(implementation = ResumeResponse.class))),
        @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ResumeResponse> getResumeById(@PathVariable Long id);

    @Operation(summary = "이력서 전체 조회", description = "모든 이력서를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이력서 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = List.class)))
    })
    ResponseEntity<List<ResumeResponse>> getAllResumes();

    @Operation(summary = "이력서 삭제", description = "이력서를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "이력서 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteResume(@PathVariable Long id);

    @Operation(summary = "이메일로 이력서 조회", description = "이메일을 사용하여 이력서를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이력서 조회 성공",
            content = @Content(schema = @Schema(implementation = ResumeResponse.class))),
        @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ResumeResponse> getResumeByEmail(@PathVariable String email);
}
