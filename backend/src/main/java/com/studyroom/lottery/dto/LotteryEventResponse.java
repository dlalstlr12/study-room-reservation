package com.studyroom.lottery.dto;

import com.studyroom.lottery.LotteryEvent;
import com.studyroom.lottery.LotteryEventStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LotteryEventResponse(
		Long id,
		String title,
		String prize,
		LocalDateTime targetAt,
		LocalDateTime drawAt,
		int winnerCount,
		LotteryEventStatus status,
		LocalDateTime drawnAt,
		long entryCount,
		/** 당첨자 이름 (DRAWN 일 때만 채워짐) */
		List<String> winners,
		/** 요청자 기준 결과 */
		MyLotteryResult myResult
) {
	public static LotteryEventResponse of(LotteryEvent event, long entryCount, List<String> winners,
			MyLotteryResult myResult) {
		return new LotteryEventResponse(
				event.getId(), event.getTitle(), event.getPrize(),
				event.getTargetAt(), event.getDrawAt(), event.getWinnerCount(),
				event.getStatus(), event.getDrawnAt(), entryCount, winners, myResult);
	}
}
