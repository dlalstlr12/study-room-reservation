package com.studyroom.ranking;

import com.studyroom.ranking.message.UsageEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 이용 이벤트를 Kafka로 발행한다. 퇴실 트랜잭션이 커밋된 뒤 호출되므로,
 * 여기서부터는 랭킹 워커의 책임이다. 발행 실패는 로그만 (원천은 {@code usage_logs}).
 */
@Component
public class UsageEventPublisher {

	public static final String DEFAULT_TOPIC = "usage-events";

	private static final Logger log = LoggerFactory.getLogger(UsageEventPublisher.class);

	private final KafkaTemplate<String, UsageEventMessage> kafkaTemplate;
	private final String topic;

	public UsageEventPublisher(KafkaTemplate<String, UsageEventMessage> kafkaTemplate,
			@Value("${ranking.topic:" + DEFAULT_TOPIC + "}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void publish(UsageEventMessage message) {
		kafkaTemplate.send(topic, message.memberId().toString(), message)
				.whenComplete((result, ex) -> {
					if (ex != null) {
						log.error("[이용 이벤트 발행 실패] reservation={}", message.reservationId(), ex);
					}
				});
	}
}
