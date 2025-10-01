package com.hiresense.resume.dto.request;

public record AcademicRecordUpdateRequest(
    String schoolName,
    String period,
    String status,
    Double gpa,
    String major
) {
}
