package com.hiresense.resume.controller;

import com.hiresense.resume.dto.request.ResumeRequest;
import com.hiresense.resume.dto.request.ResumeUpdateRequest;
import com.hiresense.resume.dto.response.ResumeResponse;
import com.hiresense.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
public class ResumeController implements ResumeApiDocs {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody ResumeRequest request) {
        ResumeResponse response = resumeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateResume(@PathVariable Long id,
                                             @Valid @RequestBody ResumeUpdateRequest request) {
        resumeService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(@PathVariable Long id) {
        ResumeResponse response = resumeService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getAllResumes() {
        List<ResumeResponse> responses = resumeService.findAll();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
