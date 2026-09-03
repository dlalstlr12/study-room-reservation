package com.studyroom.notification;

/** 알림 발송 상태. */
public enum NotificationStatus {

	/** 워커가 발송에 성공하고 이력을 남긴 상태 */
	SENT,
	/** 재시도를 모두 소진하고 DLT 핸들러가 남긴 최종 실패 */
	FAILED
}
