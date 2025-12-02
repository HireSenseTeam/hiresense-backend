package com.hiresense.jobPosting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.user.domain.User;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByUser(User user);
}
