package com.hiresense.global.error;

public record ErrorResponse(
        String message,
        String code
) {
    public static ErrorResponse of(final ErrorCode code) {
        return new ErrorResponse(code.getMessage(), code.getCode());
    }

    public static ErrorResponse of(final ErrorCode code, final String message) {
        return new ErrorResponse(message, code.getCode());
    }
}
