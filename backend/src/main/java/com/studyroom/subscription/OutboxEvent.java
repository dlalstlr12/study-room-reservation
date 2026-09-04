package com.studyroom.subscription;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 트랜잭션 아웃박스. 도메인 변경과 <b>같은 트랜잭션</b>에서 저장되고, 릴레이가 읽어 Kafka로 발행한다.
 * 커밋됐다면 이벤트는 반드시 outbox에 있고, 브로커가 죽어도 다음 폴에서 재발행된다.
 */
@Entity
@Getter
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "aggregate_type", nullable = false, length = 40)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private Long aggregateId;

	@Column(name = "event_type", nullable = false, length = 40)
	private String eventType;

	@Lob
	@Column(nullable = false)
	private String payload;

	private LocalDateTime publishedAt;

	private OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
	}

	public static OutboxEvent of(String aggregateType, Long aggregateId, String eventType,
			String payload) {
		return new OutboxEvent(aggregateType, aggregateId, eventType, payload);
	}

	public void markPublished() {
		this.publishedAt = LocalDateTime.now();
	}
}
