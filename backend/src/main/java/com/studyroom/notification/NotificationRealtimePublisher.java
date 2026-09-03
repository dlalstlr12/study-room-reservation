package com.studyroom.notification;

import com.studyroom.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 저장된 알림을 회원에게 즉시 푸시한다. {@code /topic/notifications/{memberId}} —
 * 룸 현황({@code /topic/rooms/{id}})과 같은 방식이라 STOMP 세션 인증 없이 동작한다.
 * 푸시 실패가 워커 처리(이력 저장)를 되돌리지 않도록 삼켜서 로그만 남긴다.
 */
@Component
public class NotificationRealtimePublisher {

	private static final Logger log = LoggerFactory.getLogger(NotificationRealtimePublisher.class);

	private final SimpMessagingTemplate messaging;

	public NotificationRealtimePublisher(SimpMessagingTemplate messaging) {
		this.messaging = messaging;
	}

	public void push(Long memberId, NotificationResponse payload) {
		try {
			messaging.convertAndSend("/topic/notifications/" + memberId, payload);
		} catch (RuntimeException e) {
			log.warn("알림 푸시 실패: member={}", memberId, e);
		}
	}
}
