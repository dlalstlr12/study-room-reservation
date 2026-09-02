package com.studyroom.lottery.dto;

import java.time.LocalDateTime;
import java.util.List;

/** {@code /topic/lottery} 로 브로드캐스트되는 추첨 결과 (뷰어 무관). */
public record LotteryResultMessage(
		Long eventId,
		String title,
		String prize,
		List<String> winners,
		LocalDateTime drawnAt
) {
}
