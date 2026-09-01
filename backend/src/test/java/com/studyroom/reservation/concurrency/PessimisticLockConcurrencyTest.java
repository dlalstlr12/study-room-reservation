package com.studyroom.reservation.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.ConcurrencyScenarioSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DB 비관적 락(룸 행 FOR UPDATE): 같은 룸의 예약 생성이 직렬화되어 겹침이 하나만 통과한다.
 */
@SpringBootTest(properties = "reservation.lock.strategy=pessimistic")
class PessimisticLockConcurrencyTest extends ConcurrencyScenarioSupport {

	private static final int THREADS = 50;

	@Test
	@DisplayName("비관적 락: 동시 요청 50건 중 정확히 1건만 예약된다")
	void only_one_succeeds() {
		long roomId = newRoom().getId();
		List<Long> members = newMembers(THREADS);

		Result result = runConcurrentCreates(roomId, members, THREADS);

		assertThat(result.success()).isEqualTo(1);
		assertThat(result.conflict()).isEqualTo(THREADS - 1);
		assertThat(result.other()).isZero();
		assertThat(reservedCount(roomId)).isEqualTo(1);
	}
}
