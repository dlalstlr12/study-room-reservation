package com.studyroom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.notification.message.NotificationMessage;
import com.studyroom.support.NotificationScenarioSupport;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 워커가 메시지를 소비해 이력을 남기고, 재처리에도 중복 저장하지 않는다. */
class NotificationConsumerTest extends NotificationScenarioSupport {

	@Test
	@DisplayName("메시지 발행 → notifications 행 1개 SENT")
	void consumes_and_records() {
		Long memberId = newMember();
		String dedupKey = uniqueKey();

		notificationEventPublisher.publish(new NotificationMessage(
				NotificationType.ANNOUNCEMENT, memberId, "점검 안내", "오늘 02:00 점검", null, dedupKey));

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			List<Notification> rows = notificationRepository.findAll().stream()
					.filter(n -> n.getDedupKey().equals(dedupKey))
					.toList();
			assertThat(rows).hasSize(1);
			assertThat(rows.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
			assertThat(rows.get(0).getMemberId()).isEqualTo(memberId);
		});
	}

	@Test
	@DisplayName("같은 dedupKey 재발행 → 여전히 1개 (멱등)")
	void idempotent_on_redelivery() throws InterruptedException {
		Long memberId = newMember();
		String dedupKey = uniqueKey();
		NotificationMessage message = new NotificationMessage(
				NotificationType.ANNOUNCEMENT, memberId, "공지", "본문", null, dedupKey);

		notificationEventPublisher.publish(message);
		await().atMost(Duration.ofSeconds(15)).until(() ->
				notificationRepository.existsByDedupKey(dedupKey));

		notificationEventPublisher.publish(message);
		notificationEventPublisher.publish(message);
		Thread.sleep(3000); // 재처리가 흘러갈 시간을 준다

		long count = notificationRepository.findAll().stream()
				.filter(n -> n.getDedupKey().equals(dedupKey))
				.count();
		assertThat(count).isEqualTo(1);
	}
}
