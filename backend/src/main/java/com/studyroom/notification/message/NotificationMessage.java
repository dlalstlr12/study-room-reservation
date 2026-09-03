package com.studyroom.notification.message;

import com.studyroom.notification.NotificationType;

/**
 * Kafka {@code notification-events} 토픽의 메시지. 회원 한 명에게 보낼 알림 한 건을 나타낸다.
 * 파티션 키는 {@code memberId} — 같은 회원의 알림 순서를 보장한다.
 *
 * @param dedupKey 멱등 키. 워커가 저장 전에 중복을 걸러낸다 (예: {@code lottery:42:7}).
 */
public record NotificationMessage(
		NotificationType type,
		Long memberId,
		String title,
		String body,
		Long refId,
		String dedupKey
) {
}
