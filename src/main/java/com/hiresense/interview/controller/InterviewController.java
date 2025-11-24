package com.hiresense.interview.controller;

import com.hiresense.interview.dto.request.InterviewAnswerRequest;
import com.hiresense.interview.dto.request.InterviewStartRequest;
import com.hiresense.interview.dto.response.InterviewAnswerDetailResponse;
import com.hiresense.interview.dto.response.InterviewAnswerResponse;
import com.hiresense.interview.dto.response.InterviewScoreResponse;
import com.hiresense.interview.dto.response.InterviewSessionResponse;
import com.hiresense.interview.dto.response.InterviewStartResponse;
import com.hiresense.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController implements InterviewApiDocs {
    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(@Valid @RequestBody InterviewStartRequest request) {
        InterviewStartResponse response = interviewService.startInterview(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<InterviewAnswerResponse> handleAnswer(@Valid @RequestBody InterviewAnswerRequest request) {
        InterviewAnswerResponse response = interviewService.handleAnswer(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/score")
    public ResponseEntity<InterviewScoreResponse> getScore(@RequestParam String sessionId) {
        InterviewScoreResponse response = interviewService.getScore(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<InterviewSessionResponse> getSession(@PathVariable String sessionId) {
        InterviewSessionResponse response = interviewService.getSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}/answers")
    public ResponseEntity<List<InterviewAnswerDetailResponse>> getAnswers(@PathVariable String sessionId) {
        List<InterviewAnswerDetailResponse> responses = interviewService.getAnswers(sessionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<InterviewSessionResponse>> getSessions(
            @RequestParam(required = false) String applicantEmail,
            @RequestParam(required = false) Long jobPostingId) {
        List<InterviewSessionResponse> responses;
        if (applicantEmail != null) {
            responses = interviewService.getSessionsByApplicant(applicantEmail);
        } else if (jobPostingId != null) {
            responses = interviewService.getSessionsByJobPosting(jobPostingId);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(responses);
    }
}
