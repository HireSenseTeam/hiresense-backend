package com.hiresense.resume.repository;

import com.hiresense.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByEmail(String email);
}
