package com.studyroom.reservation.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 특정 룸의 하루 예약 현황. 예약(RESERVED)과 홀딩(HOLDING)을 시작 시각 순으로 합친다.
 */
public record RoomScheduleResponse(
		Long roomId,
		String roomName,
		LocalDate date,
		List<Entry> entries
) {

	public record Entry(
			ScheduleEntryType type,
			LocalDateTime startAt,
			LocalDateTime endAt,
			boolean mine
	) {
	}
}
