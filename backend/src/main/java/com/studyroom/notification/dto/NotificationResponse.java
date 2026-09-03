package com.studyroom.notification.dto;

import com.studyroom.notification.Notification;
import com.studyroom.notification.NotificationStatus;
import com.studyroom.notification.NotificationType;
import java.time.LocalDateTime;

/** 알림 한 건. 목록 조회와 WebSocket 푸시가 공유한다. */
public record NotificationResponse(
		Long id,
		NotificationType type,
		String title,
		String body,
		Long refId,
		NotificationStatus status,
		boolean read,
		LocalDateTime createdAt
) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getBody(),
				notification.getRefId(),
				notification.getStatus(),
				notification.isRead(),
				notification.getCreatedAt());
	}
}
