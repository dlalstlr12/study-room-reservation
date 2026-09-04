package com.studyroom.reservation;

import java.time.LocalDateTime;

/**
 * 예약이 퇴실(COMPLETED)됐을 때 발행되는 in-process 이벤트.
 * 랭킹 도메인이 AFTER_COMMIT으로 구독해 Kafka {@code usage-events} 로 넘긴다.
 */
public record ReservationCompletedEvent(
		Long reservationId,
		Long memberId,
		Long roomId,
		int minutes,
		LocalDateTime occurredAt
) {
}
