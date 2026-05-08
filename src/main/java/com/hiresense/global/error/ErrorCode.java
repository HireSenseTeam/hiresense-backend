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
    SCORING_IN_PROGRESS(HttpStatus.ACCEPTED, "I001", "채점이 아직 진행 중입니다. 잠시 후 다시 시도해주세요."),
    SCORING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I002", "채점 처리 중 오류가 발생했습니다."),
    SCORING_DATA_INVALID(HttpStatus.BAD_REQUEST, "I003", "채점 데이터 형식이 올바르지 않습니다."),
    INTERVIEW_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "I004", "면접 세션을 찾을 수 없습니다."),
    INTERVIEW_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "I005", "이미 진행 중인 면접이 있습니다."),
    INTERVIEW_INVALID_STATUS(HttpStatus.CONFLICT, "I006", "현재 면접 상태에서 수행할 수 없는 요청입니다."),
    INTERVIEW_NO_QUESTIONS(HttpStatus.CONFLICT, "I007", "면접에 사용할 질문이 없습니다."),
    INTERVIEW_ALREADY_COMPLETED(HttpStatus.CONFLICT, "I008", "이미 모든 질문에 답변했습니다."),
    SCORING_ALREADY_EXISTS(HttpStatus.CONFLICT, "I009", "이미 채점 결과가 저장된 면접입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
