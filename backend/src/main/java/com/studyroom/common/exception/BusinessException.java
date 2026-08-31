package com.studyroom.common.exception;

/**
 * 도메인 규칙 위반을 나타내는 예외. 서비스 계층에서 던지고
 * {@link GlobalExceptionHandler}가 {@link ErrorCode}에 담긴 상태로 응답한다.
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
