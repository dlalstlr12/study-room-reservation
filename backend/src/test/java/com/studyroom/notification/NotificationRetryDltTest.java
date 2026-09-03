package com.studyroom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.notification.message.NotificationMessage;
import com.studyroom.support.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 발송이 계속 실패하면 재시도({@code -retry-*})를 소진한 뒤 DLT로 격리되고, 이력이 FAILED로 남는다.
 *
 * <p>발송 실패율 100% + 이 테스트 전용 토픽으로 실행해 다른 테스트와 완전히 격리한다.
 */
@SpringBootTest(properties = {
		"notification.delivery.failure-rate=1.0",
		"notification.topic=notification-events-retrytest",
		"notification.consumer.group-id=notif-retry-dlt-test"
})
class NotificationRetryDltTest extends IntegrationTest {

	private static final String DLT_TOPIC = "notification-events-retrytest-dlt";

	@Autowired
	NotificationEventPublisher publisher;
	@Autowired
	NotificationRepository notificationRepository;
	@org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}")
	String bootstrapServers;

	@AfterEach
	void clear() {
		notificationRepository.deleteAll();
	}

	@Test
	@DisplayName("재시도 소진 → DLT 격리 + 이력 FAILED")
	void exhausts_retries_then_dlt() {
		String dedupKey = "retry-dlt:" + UUID.randomUUID();

		try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
			dltConsumer.subscribe(List.of(DLT_TOPIC));
			dltConsumer.poll(Duration.ofMillis(500)); // 파티션 배정

			publisher.publish(new NotificationMessage(
					NotificationType.LOTTERY_WON, 1L, "당첨", "축하합니다", 1L, dedupKey));

			await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> {
				boolean landed = false;
				for (ConsumerRecord<String, String> record : dltConsumer.poll(Duration.ofSeconds(1))) {
					if (record.value() != null && record.value().contains(dedupKey)) {
						landed = true;
					}
				}
				assertThat(landed).as("메시지가 DLT로 격리됨").isTrue();
			});
		}

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			List<Notification> rows = notificationRepository.findAll().stream()
					.filter(n -> n.getDedupKey().equals(dedupKey))
					.toList();
			assertThat(rows).hasSize(1);
			assertThat(rows.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
		});
	}

	private KafkaConsumer<String, String> dltConsumer() {
		Map<String, Object> props = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
				ConsumerConfig.GROUP_ID_CONFIG, "dlt-assert-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return new KafkaConsumer<>(props);
	}
}
