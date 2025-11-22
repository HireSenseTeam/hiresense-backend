package com.hiresense.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력 값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 요청 방식입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 데이터를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입의 값입니다"),

    // 인증
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED, "A002", "비활성화된 계정입니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),

    // 이력서
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "이력서를 찾을 수 없습니다"),

    // 채용공고
    JOB_POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "J001", "채용공고를 찾을 수 없습니다"),

    // 면접
    SCORING_IN_PROGRESS(HttpStatus.ACCEPTED, "I001", "채점이 아직 진행 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
