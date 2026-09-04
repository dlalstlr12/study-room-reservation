package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.RankingScenarioSupport;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ZINCRBY 는 단일 명령이라 동시 갱신에도 점수가 정확하다. */
class RankingConcurrencyTest extends RankingScenarioSupport {

	@Test
	@DisplayName("10 스레드 × 각 20회 add → 최종 점수 정확히 200")
	void concurrent_increments_are_exact() throws InterruptedException {
		Long memberId = 42L;
		int threads = 10;
		int perThread = 20;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int t = 0; t < threads; t++) {
			pool.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < perThread; i++) {
						rankingRepository.add(RankingScope.ALL, null, memberId, 1);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
		pool.shutdownNow();

		assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId))
				.isEqualTo((long) threads * perThread);
	}
}
