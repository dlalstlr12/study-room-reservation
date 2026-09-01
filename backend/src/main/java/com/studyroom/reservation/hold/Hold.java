package com.studyroom.reservation.hold;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 좌석 홀딩 한 건. DB가 아니라 Redis에 TTL과 함께 저장된다.
 *
 * <p>값 키: {@code hold:{roomId}:{holdId}} — 만료 keyspace 이벤트가 키 이름만 주므로
 * roomId를 키에 넣어 만료 시 어느 룸 캐시를 비울지 복원할 수 있게 한다.
 */
public record Hold(
		String holdId,
		Long roomId,
		Long memberId,
		LocalDateTime startAt,
		LocalDateTime endAt,
		LocalDateTime expiresAt
) {

	public static Hold create(Long roomId, Long memberId, LocalDateTime startAt, LocalDateTime endAt,
			LocalDateTime expiresAt) {
		return new Hold(UUID.randomUUID().toString(), roomId, memberId, startAt, endAt, expiresAt);
	}

	public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
		return startAt.isBefore(otherEnd) && endAt.isAfter(otherStart);
	}

	public boolean ownedBy(Long memberId) {
		return this.memberId.equals(memberId);
	}
}
