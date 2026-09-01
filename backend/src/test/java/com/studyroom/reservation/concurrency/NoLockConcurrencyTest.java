package com.studyroom.reservation.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.ConcurrencyScenarioSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 락이 없을 때 같은 룸·같은 시간대에 동시 예약이 들어오면 겹치는 예약이 함께 저장된다(오버부킹).
 * 이 테스트는 그 버그를 재현한다. 2단계에서 비관적/분산 락으로 해결한다.
 */
@SpringBootTest(properties = "reservation.lock.strategy=none")
class NoLockConcurrencyTest extends ConcurrencyScenarioSupport {

	private static final Logger log = LoggerFactory.getLogger(NoLockConcurrencyTest.class);
	private static final int THREADS = 50;

	@Test
	@DisplayName("락 없음: 동시 요청 시 겹치는 예약이 여러 건 생성된다 (오버부킹 재현)")
	void reproduces_overbooking() {
		// check-then-act 레이스는 확률적이라 몇 번 반복해 재현한다.
		long maxReserved = 0;
		for (int attempt = 1; attempt <= 3; attempt++) {
			long roomId = newRoom().getId();
			List<Long> members = newMembers(THREADS);

			Result result = runConcurrentCreates(roomId, members, THREADS);
			long reserved = reservedCount(roomId);
			maxReserved = Math.max(maxReserved, reserved);

			log.warn("[재현 시도 {}] {}스레드 동시 요청 → 성공 {}, 충돌 {}, 기타 {} / RESERVED {}건",
					attempt, THREADS, result.success(), result.conflict(), result.other(), reserved);

			assertThat(result.success() + result.conflict() + result.other()).isEqualTo(THREADS);
			if (reserved > 1) {
				break;
			}
		}

		assertThat(maxReserved)
				.as("락이 없으면 겹치는 예약이 2건 이상 저장되어야 한다 (버그 재현)")
				.isGreaterThan(1);
	}
}
