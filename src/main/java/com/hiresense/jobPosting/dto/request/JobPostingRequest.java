package com.hiresense.jobPosting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JobPostingRequest(
    @NotBlank(message = "회사명은 필수 입력값입니다.")
    String companyName,
    @NotBlank(message = "직무는 필수 입력값입니다.")
    String jobTitle,
    @NotBlank(message = "근무지는 필수 입력값입니다.")
    String workLocation,
    @NotBlank(message = "모집 기간은 필수 입력값입니다.")
    String recruitmentPeriod,
    @NotBlank(message = "자격 요건은 필수 입력값입니다.")
    String qualifications,
    @NotBlank(message = "인재상은 필수 입력값입니다.")
    String idealCandidate,
    @NotBlank(message = "우대사항은 필수 입력값입니다.")
    String preferredQualifications,
    @NotBlank(message = "주요 업무는 필수 입력값입니다.")
    String jobDescription
) {
}
