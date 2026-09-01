package com.studyroom.reservation.hold;

import java.time.LocalDateTime;

public record HoldResponse(
		String holdId,
		Long roomId,
		String roomName,
		LocalDateTime startAt,
		LocalDateTime endAt,
		LocalDateTime expiresAt
) {
	public static HoldResponse from(Hold hold, String roomName) {
		return new HoldResponse(hold.holdId(), hold.roomId(), roomName,
				hold.startAt(), hold.endAt(), hold.expiresAt());
	}
}
