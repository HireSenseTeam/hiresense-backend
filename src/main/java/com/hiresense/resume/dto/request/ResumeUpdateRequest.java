package com.hiresense.resume.dto.request;

import jakarta.validation.Valid;

public record ResumeUpdateRequest(
    String name,
    String address,
    String gender,
    String birthYear,
    String phone,
    String email,
    String homePhone,
    @Valid
    AcademicRecordUpdateRequest academicRecord,
    @Valid
    JobPreferenceUpdateRequest jobPreference,
    String desiredRegion,
    Integer desiredSalary,
    @Valid
    WorkConditionUpdateRequest workCondition
) {
}
