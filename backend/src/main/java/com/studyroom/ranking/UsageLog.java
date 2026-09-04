package com.studyroom.ranking;

import com.studyroom.common.entity.BaseTimeEntity;
import com.studyroom.ranking.message.UsageEventMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이용(퇴실) 한 건. 랭킹 Sorted Set 의 원천이자, {@code reservation_id} UNIQUE 로
 * at-least-once 재처리의 중복 집계를 막는 멱등 가드다.
 */
@Entity
@Getter
@Table(name = "usage_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(nullable = false)
	private int minutes;

	@Column(nullable = false)
	private LocalDateTime occurredAt;

	private UsageLog(Long reservationId, Long memberId, Long roomId, int minutes,
			LocalDateTime occurredAt) {
		this.reservationId = reservationId;
		this.memberId = memberId;
		this.roomId = roomId;
		this.minutes = minutes;
		this.occurredAt = occurredAt;
	}

	public static UsageLog of(UsageEventMessage message) {
		return new UsageLog(message.reservationId(), message.memberId(), message.roomId(),
				message.minutes(), message.occurredAt());
	}
}
