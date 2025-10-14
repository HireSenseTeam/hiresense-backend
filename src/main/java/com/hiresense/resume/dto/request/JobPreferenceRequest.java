package com.hiresense.resume.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JobPreferenceRequest(
    @NotBlank(message = "희망 직무는 필수 입력값입니다.")
    String desiredJob,
    String experienceLevel,
    String description
) {
}
