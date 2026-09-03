package com.studyroom.notification;

import com.studyroom.notification.message.NotificationMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 요청을 Kafka로 발행한다. 도메인 트랜잭션이 커밋된 뒤(AFTER_COMMIT) 호출되므로
 * 여기서부터는 워커의 책임이다.
 *
 * <p>발행 자체가 실패하면(브로커 다운 등) 로그만 남고 유실될 수 있다 —
 * 트랜잭션 아웃박스로 이 틈을 막는 것은 로드맵 8단계에서 다룬다.
 */
@Component
public class NotificationEventPublisher {

	/** 기본 토픽 이름. 소비 쪽({@code NotificationConsumer})과 같은 프로퍼티를 본다. */
	public static final String DEFAULT_TOPIC = "notification-events";

	private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

	private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
	private final String topic;

	public NotificationEventPublisher(KafkaTemplate<String, NotificationMessage> kafkaTemplate,
			@Value("${notification.topic:" + DEFAULT_TOPIC + "}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void publish(NotificationMessage message) {
		kafkaTemplate.send(topic, message.memberId().toString(), message)
				.whenComplete((result, ex) -> {
					if (ex != null) {
						log.error("[알림 발행 실패] dedup={}", message.dedupKey(), ex);
					}
				});
	}

	public void publishAll(List<NotificationMessage> messages) {
		messages.forEach(this::publish);
	}
}
