package com.hiresense.resume.service;

import com.hiresense.global.error.exception.ResumeNotFoundException;
import com.hiresense.resume.domain.Resume;
import com.hiresense.resume.dto.request.ResumeRequest;
import com.hiresense.resume.dto.request.ResumeUpdateRequest;
import com.hiresense.resume.dto.response.ResumeResponse;
import com.hiresense.resume.repository.ResumeRepository;
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
public class ResumeService {

    private final ResumeRepository resumeRepository;

    @Transactional
    public ResumeResponse create(ResumeRequest request) {
        log.info("이력서 생성을 시작합니다. name: {}", request.name());
        Resume resume = Resume.createFrom(request);
        Resume savedResume = resumeRepository.save(resume);
        log.info("이력서 생성이 완료되었습니다. id: {}", savedResume.getId());
        return ResumeResponse.from(savedResume);
    }

    @Transactional
    public void update(Long id, ResumeUpdateRequest request) {
        log.info("ID {} 이력서 수정을 시작합니다.", id);
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(ResumeNotFoundException::new);
        resume.updateFrom(request);
        log.info("ID {} 이력서 수정이 완료되었습니다.", id);
    }

    public ResumeResponse findById(Long id) {
        log.info("ID {} 이력서 조회를 시작합니다.", id);
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(ResumeNotFoundException::new);
        log.info("ID {} 이력서 조회가 완료되었습니다.", id);
        return ResumeResponse.from(resume);
    }

    public List<ResumeResponse> findAll() {
        log.info("모든 이력서 조회를 시작합니다.");
        List<Resume> resumes = resumeRepository.findAll();
        log.info("총 {}개의 이력서 조회가 완료되었습니다.", resumes.size());
        return resumes.stream()
            .map(ResumeResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        log.info("ID {} 이력서 삭제를 시작합니다.", id);
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(ResumeNotFoundException::new);
        resumeRepository.delete(resume);
        log.info("ID {} 이력서 삭제가 완료되었습니다.", id);
    }
}
