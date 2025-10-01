package com.hiresense.resume.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AcademicRecordRequest(
    @NotBlank(message = "학교명은 필수 입력값입니다.")
    String schoolName,
    @NotBlank(message = "재학 기간은 필수 입력값입니다.")
    String period,
    @NotBlank(message = "재학 상태는 필수 입력값입니다.")
    String status,
    Double gpa,
    String major
) {
}
