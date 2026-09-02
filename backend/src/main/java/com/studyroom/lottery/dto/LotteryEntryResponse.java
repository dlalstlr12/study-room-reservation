package com.studyroom.lottery.dto;

import com.studyroom.lottery.LotteryEntry;
import java.time.LocalDateTime;

/** 내 참여 이력 한 건. */
public record LotteryEntryResponse(
		Long eventId,
		String eventTitle,
		String prize,
		boolean winner,
		LocalDateTime drawnAt
) {
	public static LotteryEntryResponse from(LotteryEntry entry) {
		return new LotteryEntryResponse(
				entry.getEvent().getId(),
				entry.getEvent().getTitle(),
				entry.getEvent().getPrize(),
				entry.isWinner(),
				entry.getEvent().getDrawnAt());
	}
}
