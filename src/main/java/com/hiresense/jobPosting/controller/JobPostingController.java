package com.hiresense.jobPosting.controller;

import com.hiresense.jobPosting.dto.request.JobPostingRequest;
import com.hiresense.jobPosting.dto.request.JobPostingUpdateRequest;
import com.hiresense.jobPosting.dto.response.JobPostingResponse;
import com.hiresense.jobPosting.service.JobPostingService;
import com.hiresense.question.dto.response.QuestionResponse;
import com.hiresense.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/job-postings")
@RequiredArgsConstructor
public class JobPostingController implements JobPostingApiDocs {

    private final JobPostingService jobPostingService;
    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<JobPostingResponse> createJobPosting(@Valid @RequestBody JobPostingRequest request) {
        JobPostingResponse response = jobPostingService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/job-postings/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> getAllJobPostings() {
        List<JobPostingResponse> responses = jobPostingService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getJobPostingById(@PathVariable Long id) {
        JobPostingResponse response = jobPostingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateJobPosting(@PathVariable Long id,
                                             @Valid @RequestBody JobPostingUpdateRequest request) {
        jobPostingService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobPosting(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable Long id) {
        List<QuestionResponse> responses = questionService.findByJobPostingId(id);
        return ResponseEntity.ok(responses);
    }
}
