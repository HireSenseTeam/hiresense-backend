package com.hiresense.jobPosting.service;

import com.hiresense.ai.service.QuestionGenerationService;
import com.hiresense.global.error.exception.JobPostingNotFoundException;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.jobPosting.dto.request.JobPostingRequest;
import com.hiresense.jobPosting.dto.request.JobPostingUpdateRequest;
import com.hiresense.jobPosting.dto.response.JobPostingResponse;
import com.hiresense.jobPosting.repository.JobPostingRepository;
import com.hiresense.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingService {
    private final JobPostingRepository jobPostingRepository;
    private final QuestionGenerationService questionGenerationService;

    @Transactional
    public JobPostingResponse create(JobPostingRequest request, User user) {
        log.info("채용공고 생성을 시작합니다. companyName: {}, userId: {}", request.companyName(), user.getId());
        JobPosting jobPosting = JobPosting.createJobPosting(request, user);
        JobPosting savedJobPosting = jobPostingRepository.save(jobPosting);
        log.info("채용공고 생성이 완료되었습니다. id: {}", savedJobPosting.getId());
        
        questionGenerationService.generateJobPostingQuestions(savedJobPosting.getId())
                .thenAccept(questions -> {
                    log.info("채용공고 질문 생성 완료: jobPostingId={}, 질문 수={}", 
                            savedJobPosting.getId(), questions.size());
                })
                .exceptionally(ex -> {
                    log.error("채용공고 질문 생성 중 오류 발생: jobPostingId={}, error={}", 
                            savedJobPosting.getId(), ex.getMessage(), ex);
                    return null;
                });
        
        return JobPostingResponse.from(savedJobPosting);
    }

    public JobPostingResponse findById(Long id) {
        log.info("ID {} 채용공고 조회를 시작합니다.", id);
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(JobPostingNotFoundException::new);
        log.info("ID {} 채용공고 조회가 완료되었습니다.", id);
        return JobPostingResponse.from(jobPosting);
    }

    public List<JobPostingResponse> findAll() {
        log.info("모든 채용공고 조회를 시작합니다.");
        List<JobPosting> jobPostings = jobPostingRepository.findAll();
        log.info("총 {}개의 채용공고 조회가 완료되었습니다.", jobPostings.size());
        return jobPostings.stream()
                .map(JobPostingResponse::from)
                .collect(Collectors.toList());
    }

    public List<JobPostingResponse> findByUser(User user) {
        log.info("사용자별 채용공고 조회를 시작합니다. userId: {}", user.getId());
        List<JobPosting> jobPostings = jobPostingRepository.findByUser(user);
        log.info("사용자 {}의 채용공고 {}개 조회가 완료되었습니다.", user.getId(), jobPostings.size());
        return jobPostings.stream()
                .map(JobPostingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long id, JobPostingUpdateRequest request) {
        log.info("ID {} 채용공고 수정을 시작합니다.", id);
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(JobPostingNotFoundException::new);
        jobPosting.updateJobPosting(request);
        log.info("ID {} 채용공고 수정이 완료되었습니다.", id);
    }

    @Transactional
    public void delete(Long id) {
        log.info("ID {} 채용공고 삭제를 시작합니다.", id);
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(JobPostingNotFoundException::new);
        jobPostingRepository.delete(jobPosting);
        log.info("ID {} 채용공고 삭제가 완료되었습니다.", id);
    }
}
