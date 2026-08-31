package com.studyroom.common.exception;

import java.util.List;

/**
 * 표준 에러 응답 본문.
 *
 * @param code        에러 코드 문자열 (예: {@code RESERVATION_TIME_CONFLICT})
 * @param message     사람이 읽을 메시지
 * @param fieldErrors 입력 검증 실패 시 필드별 사유 (없으면 빈 리스트)
 */
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

	public record FieldError(String field, String reason) {
	}

	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message, List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, List.of());
	}

	public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(code, message, fieldErrors);
	}
}
