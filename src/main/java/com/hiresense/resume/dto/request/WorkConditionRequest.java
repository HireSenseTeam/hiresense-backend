package com.hiresense.resume.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WorkConditionRequest(
    @NotBlank(message = "고용 형태는 필수 입력값입니다.")
    String employmentType,
    String desiredHours
) {
}
