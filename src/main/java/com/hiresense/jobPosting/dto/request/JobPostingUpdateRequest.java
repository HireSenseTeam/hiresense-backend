package com.hiresense.jobPosting.dto.request;

public record JobPostingUpdateRequest(
    String companyName,
    String jobTitle,
    String workLocation,
    String recruitmentPeriod,
    String qualifications,
    String idealCandidate,
    String preferredQualifications,
    String jobDescription
){
}
