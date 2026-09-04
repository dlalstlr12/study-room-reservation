package com.studyroom.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 릴레이. 미발행 이벤트를 잠그고({@code FOR UPDATE SKIP LOCKED}) Kafka로 발행한 뒤
 * {@code published_at} 을 찍는다.
 *
 * <ul>
 *   <li>발행 실패 → 트랜잭션 롤백 → {@code published_at} 안 찍힘 → 다음 폴에서 재시도(at-least-once).
 *       소비자는 dedupKey 로 멱등 처리.</li>
 *   <li>{@code SKIP LOCKED} 로 릴레이 인스턴스가 여러 개여도 같은 행을 두 번 발행하지 않는다.</li>
 * </ul>
 *
 * <p>주기 실행은 {@link OutboxRelayScheduler} 가 담당한다 — 로직과 스케줄을 분리해 테스트에서
 * {@link #relay()} 를 직접 호출할 수 있다.
 */
@Component
public class OutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
	private static final int BATCH_SIZE = 100;

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, SubscriptionEventMessage> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;

	public OutboxRelay(OutboxEventRepository outboxEventRepository,
			KafkaTemplate<String, SubscriptionEventMessage> kafkaTemplate, ObjectMapper objectMapper,
			@Value("${subscription.topic:subscription-events}") String topic) {
		this.outboxEventRepository = outboxEventRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	@Transactional
	public void relay() {
		List<OutboxEvent> pending = outboxEventRepository.lockUnpublished(BATCH_SIZE);
		if (pending.isEmpty()) {
			return;
		}
		for (OutboxEvent event : pending) {
			publish(event);
			event.markPublished();
		}
		log.info("[아웃박스] {}건 발행", pending.size());
	}

	private void publish(OutboxEvent event) {
		try {
			SubscriptionEventMessage message = objectMapper.readValue(
					event.getPayload(), SubscriptionEventMessage.class);
			kafkaTemplate.send(topic, event.getAggregateId().toString(), message).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("아웃박스 발행 중단 id=" + event.getId(), e);
		} catch (ExecutionException | RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
			// 롤백 → published_at 미기록 → 다음 폴에서 재시도
			throw new IllegalStateException("아웃박스 발행 실패 id=" + event.getId(), e);
		}
	}
}
