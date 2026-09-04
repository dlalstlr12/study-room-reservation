package com.studyroom.ranking.message;

import java.time.LocalDateTime;

/**
 * Kafka {@code usage-events} 토픽의 메시지 — 이용(퇴실) 한 건. 파티션 키는 {@code memberId}.
 *
 * @param reservationId 멱등 키 (usage_logs UNIQUE)
 * @param minutes       실제 이용 분
 * @param occurredAt    퇴실 시각 (일간 랭킹 버킷 기준)
 */
public record UsageEventMessage(
		Long reservationId,
		Long memberId,
		Long roomId,
		int minutes,
		LocalDateTime occurredAt
) {
}
