package com.studyroom.reservation.entity;

public enum ReservationStatus {
	/** 예약 확정 (1단계에서는 생성 즉시 이 상태) */
	RESERVED,
	/** 사용자가 취소함 */
	CANCELLED,
	/** 이용 완료 (퇴실) — 로드맵 후속 단계에서 사용 */
	COMPLETED
}
