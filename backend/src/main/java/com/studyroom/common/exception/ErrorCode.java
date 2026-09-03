package com.studyroom.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인 예외 코드. 각 값은 응답 코드 문자열과 HTTP 상태, 기본 메시지를 갖는다.
 */
public enum ErrorCode {

	// 공통
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예상하지 못한 오류가 발생했습니다."),

	// 회원 / 인증
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

	// 룸
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "룸을 찾을 수 없습니다."),

	// 예약
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
	INVALID_RESERVATION_TIME(HttpStatus.BAD_REQUEST, "예약 시간이 올바르지 않습니다."),
	RESERVATION_TIME_CONFLICT(HttpStatus.CONFLICT, "해당 시간에 이미 예약이 있습니다."),
	RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 예약만 접근할 수 있습니다."),
	RESERVATION_NOT_CANCELABLE(HttpStatus.CONFLICT, "취소할 수 없는 상태의 예약입니다."),
	RESERVATION_LOCK_TIMEOUT(HttpStatus.CONFLICT, "요청이 몰려 예약을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요."),

	// 홀딩 (로드맵 3단계)
	RESERVATION_HOLD_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 해당 시간대를 홀딩 중입니다. 잠시 후 다시 시도해 주세요."),
	RESERVATION_HOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "홀딩이 만료되었거나 존재하지 않습니다."),

	// 이벤트 추첨 (로드맵 5단계)
	LOTTERY_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "추첨 이벤트를 찾을 수 없습니다."),
	LOTTERY_ALREADY_DRAWN(HttpStatus.CONFLICT, "이미 추첨이 완료된 이벤트입니다."),
	LOTTERY_INVALID_SCHEDULE(HttpStatus.BAD_REQUEST, "추첨 이벤트 설정이 올바르지 않습니다."),

	// 알림 (로드맵 6단계)
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
