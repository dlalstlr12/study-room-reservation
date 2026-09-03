package com.studyroom.notification;

/** 알림 종류. 프론트는 이 값으로 아이콘·강조를 구분한다. */
public enum NotificationType {

	/** 추첨 당첨 */
	LOTTERY_WON,
	/** 추첨 미당첨 */
	LOTTERY_LOST,
	/** 전체 공지 (ADMIN 발송) */
	ANNOUNCEMENT
}
