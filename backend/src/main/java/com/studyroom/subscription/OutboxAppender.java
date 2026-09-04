package com.studyroom.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 아웃박스 테이블에 적재한다. 호출부(결제 서비스)의 트랜잭션에 참여하므로,
 * 도메인 변경과 아웃박스 저장이 원자적으로 커밋된다.
 */
@Component
public class OutboxAppender {

	static final String AGGREGATE_TYPE = "SUBSCRIPTION";

	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public OutboxAppender(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
	}

	public void append(SubscriptionEventMessage message) {
		outboxEventRepository.save(OutboxEvent.of(
				AGGREGATE_TYPE, message.subscriptionId(), message.eventType(), serialize(message)));
	}

	private String serialize(SubscriptionEventMessage message) {
		try {
			return objectMapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("아웃박스 payload 직렬화 실패", e);
		}
	}
}
