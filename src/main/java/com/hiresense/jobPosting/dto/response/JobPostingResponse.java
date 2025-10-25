package com.hiresense.jobPosting.dto.response;

import com.hiresense.jobPosting.domain.JobPosting;

public record JobPostingResponse(
    Long id,
    String companyName,
    String jobTitle,
    String workLocation,
    String recruitmentPeriod,
    String qualifications,
    String idealCandidate,
    String preferredQualifications,
    String jobDescription
) {
    public static JobPostingResponse from(JobPosting jobPosting) {
        return new JobPostingResponse(
            jobPosting.getId(),
            jobPosting.getCompanyName(),
            jobPosting.getJobTitle(),
            jobPosting.getWorkLocation(),
            jobPosting.getRecruitmentPeriod(),
            jobPosting.getQualifications(),
            jobPosting.getIdealCandidate(),
            jobPosting.getPreferredQualifications(),
            jobPosting.getJobDescription()
        );
    }
}
