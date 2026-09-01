package com.studyroom.reservation.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.ConcurrencyScenarioSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Redisson 분산 락: 같은 룸 키에 하나만 진입하므로 겹치는 예약이 하나만 통과한다.
 */
@SpringBootTest(properties = "reservation.lock.strategy=distributed")
class DistributedLockConcurrencyTest extends ConcurrencyScenarioSupport {

	private static final int THREADS = 50;

	@Test
	@DisplayName("분산 락: 동시 요청 50건 중 정확히 1건만 예약된다")
	void only_one_succeeds() {
		long roomId = newRoom().getId();
		List<Long> members = newMembers(THREADS);

		Result result = runConcurrentCreates(roomId, members, THREADS);

		assertThat(result.success()).isEqualTo(1);
		assertThat(result.other()).isZero();
		assertThat(result.conflict()).isEqualTo(THREADS - 1);
		assertThat(reservedCount(roomId)).isEqualTo(1);
	}
}
