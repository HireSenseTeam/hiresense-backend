package com.hiresense.auth.dto.response;

import com.hiresense.user.domain.UserRole;

public record AuthResponse(
    String token,
    String refreshToken,
    Long userId,
    String email,
    String name,
    UserRole role
) {
}
