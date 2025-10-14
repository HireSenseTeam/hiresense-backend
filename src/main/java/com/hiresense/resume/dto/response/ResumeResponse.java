package com.hiresense.resume.dto.response;

import com.hiresense.resume.domain.Resume;

import java.time.LocalDateTime;

public record ResumeResponse(
    Long id,
    String name,
    String address,
    String gender,
    String birthYear,
    String phone,
    String email,
    String homePhone,
    AcademicRecordResponse academicRecord,
    JobPreferenceResponse jobPreference,
    String desiredRegion,
    Integer desiredSalary,
    WorkConditionResponse workCondition,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static ResumeResponse from(Resume resume) {
        return new ResumeResponse(
            resume.getId(),
            resume.getName(),
            resume.getAddress(),
            resume.getGender().name(),
            resume.getBirthYear(),
            resume.getPhone(),
            resume.getEmail(),
            resume.getHomePhone(),
            new AcademicRecordResponse(
                resume.getAcademicRecord().getSchoolName(),
                resume.getAcademicRecord().getPeriod(),
                resume.getAcademicRecord().getStatus().name(),
                resume.getAcademicRecord().getGpa(),
                resume.getAcademicRecord().getMajor()
            ),
            new JobPreferenceResponse(
                resume.getJobPreference().getDesiredJob(),
                resume.getJobPreference().getExperienceLevel().name(),
                resume.getJobPreference().getDescription()
            ),
            resume.getDesiredRegion(),
            resume.getDesiredSalary(),
            new WorkConditionResponse(
                resume.getWorkCondition().getEmploymentType().name(),
                resume.getWorkCondition().getDesiredHours()
            ),
            resume.getCreatedAt(),
            resume.getModifiedAt()
        );
    }
}
