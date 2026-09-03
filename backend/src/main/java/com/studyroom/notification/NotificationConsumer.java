package com.studyroom.notification;

import com.studyroom.notification.dto.NotificationResponse;
import com.studyroom.notification.message.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 알림 워커. {@code notification-events} 를 소비해 발송 → 이력 저장 → WebSocket 푸시한다.
 *
 * <ul>
 *   <li><b>멱등</b>: {@code dedup_key} 조회 + UNIQUE 제약으로 at-least-once 재처리의 중복을 막는다.</li>
 *   <li><b>재시도</b>: 발송 실패 시 {@code @RetryableTopic} 지수 백오프로 재시도
 *       ({@code notification-events-retry-*}).</li>
 *   <li><b>DLT</b>: 재시도를 모두 소진하면 {@code notification-events-dlt} 로 격리하고
 *       이력을 {@code FAILED} 로 남긴다.</li>
 * </ul>
 */
@Component
public class NotificationConsumer {

	private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

	private final NotificationSender sender;
	private final NotificationRepository notificationRepository;
	private final NotificationRealtimePublisher realtimePublisher;

	public NotificationConsumer(NotificationSender sender,
			NotificationRepository notificationRepository,
			NotificationRealtimePublisher realtimePublisher) {
		this.sender = sender;
		this.notificationRepository = notificationRepository;
		this.realtimePublisher = realtimePublisher;
	}

	@RetryableTopic(
			attempts = "${notification.retry.attempts}",
			backoff = @Backoff(
					delayExpression = "${notification.retry.backoff-ms}",
					multiplierExpression = "${notification.retry.backoff-multiplier}"),
			dltStrategy = DltStrategy.FAIL_ON_ERROR,
			autoCreateTopics = "true")
	@KafkaListener(topics = NotificationEventPublisher.TOPIC, groupId = "notification-worker")
	public void handle(NotificationMessage message) {
		if (notificationRepository.existsByDedupKey(message.dedupKey())) {
			log.debug("[알림] 이미 처리됨 dedup={}", message.dedupKey());
			return;
		}

		sender.send(message); // 실패하면 예외 → 재시도

		try {
			Notification saved = notificationRepository.save(Notification.sent(
					message.memberId(), message.type(), message.title(), message.body(),
					message.refId(), message.dedupKey()));
			realtimePublisher.push(message.memberId(), NotificationResponse.from(saved));
		} catch (DataIntegrityViolationException duplicate) {
			// 다른 재처리가 먼저 저장 — 이미 전달됐다고 보고 넘어간다.
			log.debug("[알림] 저장 경합, 중복으로 처리 dedup={}", message.dedupKey());
		}
	}

	@DltHandler
	public void toDlt(NotificationMessage message,
			@Header(KafkaHeaders.EXCEPTION_MESSAGE) String error) {
		log.error("[알림 DLT] 최종 실패 dedup={} 원인={}", message.dedupKey(), error);
		if (notificationRepository.existsByDedupKey(message.dedupKey())) {
			return;
		}
		try {
			notificationRepository.save(Notification.failed(
					message.memberId(), message.type(), message.title(), message.body(),
					message.refId(), message.dedupKey()));
		} catch (DataIntegrityViolationException ignored) {
			// 경합 — 이미 이력이 있음
		}
	}
}
