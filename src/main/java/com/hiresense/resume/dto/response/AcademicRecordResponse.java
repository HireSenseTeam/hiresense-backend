package com.hiresense.resume.dto.response;

public record AcademicRecordResponse(
    String schoolName,
    String period,
    String status,
    Double gpa,
    String major
) {
}
