package com.hiresense.jobPosting.domain;

import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.jobPosting.dto.request.JobPostingRequest;
import com.hiresense.jobPosting.dto.request.JobPostingUpdateRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyName;
    private String jobTitle;
    private String workLocation;
    private String recruitmentPeriod;
    private String qualifications;
    private String idealCandidate;
    private String preferredQualifications;
    private String jobDescription;

    @Builder
    private JobPosting(String companyName, String jobTitle, String workLocation, String recruitmentPeriod, String qualifications, String idealCandidate, String preferredQualifications, String jobDescription) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.workLocation = workLocation;
        this.recruitmentPeriod = recruitmentPeriod;
        this.qualifications = qualifications;
        this.idealCandidate = idealCandidate;
        this.preferredQualifications = preferredQualifications;
        this.jobDescription = jobDescription;
    }

    public static JobPosting createJobPosting(JobPostingRequest request) {
        return JobPosting.builder()
                .companyName(request.companyName())
                .jobTitle(request.jobTitle())
                .workLocation(request.workLocation())
                .recruitmentPeriod(request.recruitmentPeriod())
                .qualifications(request.qualifications())
                .idealCandidate(request.idealCandidate())
                .preferredQualifications(request.preferredQualifications())
                .jobDescription(request.jobDescription())
                .build();
    }

    public void updateJobPosting(JobPostingUpdateRequest request) {
        Optional.ofNullable(request.companyName()).ifPresent(name -> this.companyName = name);
        Optional.ofNullable(request.jobTitle()).ifPresent(title -> this.jobTitle = title);
        Optional.ofNullable(request.workLocation()).ifPresent(location -> this.workLocation = location);
        Optional.ofNullable(request.recruitmentPeriod()).ifPresent(period -> this.recruitmentPeriod = period);
        Optional.ofNullable(request.qualifications()).ifPresent(qualifications -> this.qualifications = qualifications);
        Optional.ofNullable(request.idealCandidate()).ifPresent(candidate -> this.idealCandidate = candidate);
        Optional.ofNullable(request.preferredQualifications()).ifPresent(preferred -> this.preferredQualifications = preferred);
        Optional.ofNullable(request.jobDescription()).ifPresent(description -> this.jobDescription = description);
    }
}
