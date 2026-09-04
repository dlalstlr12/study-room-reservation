package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/** 아웃박스 릴레이가 미발행 이벤트를 Kafka로 넘기고 published_at을 찍는다. 재실행은 재발행 안 함. */
class OutboxRelayTest extends SubscriptionScenarioSupport {

	@Autowired
	OutboxRelay outboxRelay;
	@Autowired
	OutboxAppender outboxAppender;
	@Autowired
	ObjectMapper objectMapper;
	@Value("${spring.kafka.bootstrap-servers}")
	String bootstrapServers;

	@Test
	@DisplayName("relay() → subscription-events 발행 + published_at 기록, 재실행 시 재발행 없음")
	void relays_once() {
		long subscriptionId = 777_000L + (long) (Math.random() * 1000);
		String marker = "relay-" + UUID.randomUUID();
		outboxAppender.append(new SubscriptionEventMessage(
				SubscriptionEventMessage.PAYMENT_SUCCEEDED, subscriptionId, 1L, marker, 9900,
				LocalDateTime.now(), null));

		try (KafkaConsumer<String, String> consumer = consumer()) {
			consumer.subscribe(List.of("subscription-events"));
			consumer.poll(Duration.ofMillis(300));

			outboxRelay.relay();

			boolean received = false;
			long deadline = System.currentTimeMillis() + 15_000;
			while (System.currentTimeMillis() < deadline && !received) {
				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(1))) {
					if (record.value() != null && record.value().contains(marker)) {
						received = true;
					}
				}
			}
			assertThat(received).as("subscription-events 로 발행됨").isTrue();
		}

		assertThat(outboxEventRepository.findAll())
				.filteredOn(e -> e.getPayload().contains(marker))
				.allMatch(e -> e.getPublishedAt() != null);

		long publishedBefore = outboxEventRepository.countByPublishedAtIsNull();
		outboxRelay.relay(); // 재실행
		assertThat(outboxEventRepository.countByPublishedAtIsNull()).isEqualTo(publishedBefore);
	}

	private KafkaConsumer<String, String> consumer() {
		Map<String, Object> props = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
				ConsumerConfig.GROUP_ID_CONFIG, "outbox-relay-test-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return new KafkaConsumer<>(props);
	}
}
