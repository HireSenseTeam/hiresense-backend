package com.hiresense.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token은 필수 입력값입니다.")
    String refreshToken
) {
}
