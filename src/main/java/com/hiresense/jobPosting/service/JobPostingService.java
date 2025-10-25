package com.hiresense.jobPosting.service;

import com.hiresense.global.error.exception.JobPostingNotFoundException;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.jobPosting.dto.request.JobPostingRequest;
import com.hiresense.jobPosting.dto.request.JobPostingUpdateRequest;
import com.hiresense.jobPosting.dto.response.JobPostingResponse;
import com.hiresense.jobPosting.repository.JobPostingRepository;
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

    @Transactional
    public JobPostingResponse create(JobPostingRequest request) {
        log.info("채용공고 생성을 시작합니다. companyName: {}", request.companyName());
        JobPosting jobPosting = JobPosting.createJobPosting(request);
        JobPosting savedJobPosting= jobPostingRepository.save(jobPosting);
        log.info("채용공고 생성이 완료되었습니다. id: {}", savedJobPosting.getId());
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
