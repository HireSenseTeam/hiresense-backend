package com.hiresense.resume.dto.request;

public record JobPreferenceUpdateRequest(
    String desiredJob,
    String experienceLevel,
    String description
) {
}
