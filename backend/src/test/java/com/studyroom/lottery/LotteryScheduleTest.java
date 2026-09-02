package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.support.LotteryScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** drawAt이 지나면 스케줄러가 알아서 추첨한다. */
@SpringBootTest(properties = "lottery.draw.poll-ms=500")
class LotteryScheduleTest extends LotteryScenarioSupport {

	@Test
	@DisplayName("drawAt 도래 시 스케줄러가 자동 추첨")
	void scheduler_draws_when_due() {
		LocalDateTime target = tomorrowAt(9);
		reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));

		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"자동 추첨", "상품", target, LocalDateTime.now().plusSeconds(2), 1)).id();

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
				assertThat(lotteryEventRepository.findById(eventId).orElseThrow().getStatus())
						.isEqualTo(LotteryEventStatus.DRAWN));

		assertThat(lotteryEntryRepository.countByEventIdAndWinnerTrue(eventId)).isEqualTo(1);
	}
}
