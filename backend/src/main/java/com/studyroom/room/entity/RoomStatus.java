package com.studyroom.room.entity;

public enum RoomStatus {
	/** 예약 가능 */
	AVAILABLE,
	/** 홀딩 중 (결제 대기) — 로드맵 3단계에서 사용 */
	HOLDING,
	/** 사용 중 */
	OCCUPIED
}
