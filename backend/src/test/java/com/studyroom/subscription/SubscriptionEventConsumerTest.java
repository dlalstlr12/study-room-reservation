package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.notification.Notification;
import com.studyroom.notification.NotificationRepository;
import com.studyroom.notification.NotificationType;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

/** 아웃박스가 발행한 결제 이벤트가 6단계 알림 파이프라인으로 흘러 notifications 행이 된다. */
class SubscriptionEventConsumerTest extends SubscriptionScenarioSupport {

	@Autowired
	KafkaTemplate<String, SubscriptionEventMessage> kafkaTemplate;
	@Autowired
	NotificationRepository notificationRepository;

	@AfterEach
	void clearNotifications() {
		notificationRepository.deleteAll();
	}

	@Test
	@DisplayName("PAYMENT_SUCCEEDED → SUBSCRIPTION_PAID 알림, 재발행에도 1건")
	void payment_event_becomes_notification() {
		Long memberId = newMember();
		LocalDateTime occurredAt = LocalDateTime.now();
		SubscriptionEventMessage message = new SubscriptionEventMessage(
				SubscriptionEventMessage.PAYMENT_SUCCEEDED, 4242L, memberId, "PRO", 9900, occurredAt, null);

		kafkaTemplate.send("subscription-events", memberId.toString(), message);

		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
			List<Notification> mine = notificationRepository.findAll().stream()
					.filter(n -> n.getMemberId().equals(memberId))
					.toList();
			assertThat(mine).hasSize(1);
			assertThat(mine.get(0).getType()).isEqualTo(NotificationType.SUBSCRIPTION_PAID);
		});

		kafkaTemplate.send("subscription-events", memberId.toString(), message); // 재발행
		await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(8)).untilAsserted(() ->
				assertThat(notificationRepository.findAll().stream()
						.filter(n -> n.getMemberId().equals(memberId)).count()).isEqualTo(1));
	}
}
