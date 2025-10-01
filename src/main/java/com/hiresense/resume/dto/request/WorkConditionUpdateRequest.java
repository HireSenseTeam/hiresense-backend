package com.hiresense.resume.dto.request;

public record WorkConditionUpdateRequest(
    String employmentType,
    String desiredHours
) {
}
