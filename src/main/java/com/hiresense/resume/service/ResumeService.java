package com.hiresense.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.global.error.exception.ResumeNotFoundException;
import com.hiresense.resume.domain.Resume;
import com.hiresense.resume.dto.request.ResumeRequest;
import com.hiresense.resume.dto.request.ResumeUpdateRequest;
import com.hiresense.resume.dto.response.ResumeResponse;
import com.hiresense.resume.repository.ResumeRepository;
import com.hiresense.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeResponse create(ResumeRequest request) {
        log.info("이력서 생성을 시작합니다. name: {}", request.name());
        Resume resume = Resume.createFrom(request);
        Resume savedResume = resumeRepository.save(resume);
        uploadResumeToS3(savedResume);
        log.info("이력서 생성이 완료되었습니다. id: {}", savedResume.getId());
        return ResumeResponse.from(savedResume);
    }

    @Transactional
    public void update(Long id, ResumeUpdateRequest request) {
        log.info("ID {} 이력서 수정을 시작합니다.", id);
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(ResumeNotFoundException::new);
        resume.updateFrom(request);
        uploadResumeToS3(resume);
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
        s3Service.deleteFile("resumes/" + id + ".json");
        log.info("ID {} 이력서 삭제가 완료되었습니다.", id);
    }

    private void uploadResumeToS3(Resume resume) {
        try {
            String jsonContent = objectMapper.writeValueAsString(resume);
            s3Service.uploadFile("resumes/" + resume.getId() + ".json", jsonContent.getBytes());
        } catch (IOException e) {
            log.error("S3에 이력서 업로드를 실패했습니다. id: {}", resume.getId(), e);
        }
    }
}
