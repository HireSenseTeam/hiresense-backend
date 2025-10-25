package com.hiresense.jobPosting.repository;

import com.hiresense.jobPosting.domain.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
}
