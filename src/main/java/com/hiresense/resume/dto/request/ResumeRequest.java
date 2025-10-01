package com.hiresense.resume.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResumeRequest(
    @NotBlank(message = "이름은 필수 입력값입니다.")
    String name,
    @NotBlank(message = "주소는 필수 입력값입니다.")
    String address,
    @NotBlank(message = "성별은 필수 입력값입니다.")
    String gender,
    @NotBlank(message = "생년월일은 필수 입력값입니다.")
    String birthYear,
    @NotBlank(message = "연락처는 필수 입력값입니다.")
    String phone,
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    String email,
    String homePhone,
    @Valid
    @NotNull(message = "학력 정보는 필수 입력값입니다.")
    AcademicRecordRequest academicRecord,
    @Valid
    @NotNull(message = "희망 직무 정보는 필수 입력값입니다.")
    JobPreferenceRequest jobPreference,
    String desiredRegion,
    @NotNull(message = "희망 연봉은 필수 입력값입니다.")
    Integer desiredSalary,
    @Valid
    @NotNull(message = "희망 근무 조건은 필수 입력값입니다.")
    WorkConditionRequest workCondition
) {
}
