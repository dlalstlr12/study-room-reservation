package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.ranking.message.UsageEventMessage;
import com.studyroom.support.RankingScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 랭킹 워커가 이용 이벤트를 집계하고, 재처리에도 점수를 두 번 올리지 않는다. */
class UsageEventConsumerTest extends RankingScenarioSupport {

	@Autowired
	UsageEventPublisher publisher;

	@Test
	@DisplayName("이용 이벤트 → usage_logs 1행 + Sorted Set 점수")
	void aggregates_usage() {
		Long memberId = newMember();
		long reservationId = uniqueReservationId();

		publisher.publish(new UsageEventMessage(reservationId, memberId, 1L, 90, LocalDateTime.now()));

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(usageLogRepository.existsByReservationId(reservationId)).isTrue();
			assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId)).isEqualTo(90);
		});
	}

	@Test
	@DisplayName("같은 reservationId 재발행 → 점수 안 늘어남 (멱등)")
	void idempotent_on_redelivery() throws InterruptedException {
		Long memberId = newMember();
		long reservationId = uniqueReservationId();
		UsageEventMessage message = new UsageEventMessage(
				reservationId, memberId, 1L, 45, LocalDateTime.now());

		publisher.publish(message);
		await().atMost(Duration.ofSeconds(15))
				.until(() -> rankingRepository.scoreOf(RankingScope.ALL, null, memberId) == 45);

		publisher.publish(message);
		publisher.publish(message);
		Thread.sleep(3000);

		assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId)).isEqualTo(45);
	}

	private long uniqueReservationId() {
		return ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
	}
}
