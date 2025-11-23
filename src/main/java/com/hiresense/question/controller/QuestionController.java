package com.hiresense.question.controller;

import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.dto.response.QuestionResponse;
import com.hiresense.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController implements QuestionApiDocs {

    private final QuestionService questionService;

    @GetMapping("/type/{type}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByType(@PathVariable QuestionType type) {
        List<QuestionResponse> responses = questionService.findByType(type);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/job-posting/{jobPostingId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByJobPosting(@PathVariable Long jobPostingId) {
        List<QuestionResponse> responses = questionService.findByJobPostingId(jobPostingId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByResume(@PathVariable Long resumeId) {
        List<QuestionResponse> responses = questionService.findByResumeId(resumeId);
        return ResponseEntity.ok(responses);
    }
}
