package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 순수 추첨 로직 — 재현성과 공정성 분포. */
class LotteryTest {

	private static final Logger log = LoggerFactory.getLogger(LotteryTest.class);

	@Test
	@DisplayName("같은 (후보, seed, 인원)이면 순서와 무관하게 같은 당첨자")
	void reproducible_regardless_of_input_order() {
		List<Long> a = List.of(5L, 1L, 3L, 9L, 7L, 2L);
		List<Long> b = List.of(9L, 7L, 5L, 3L, 2L, 1L); // 같은 집합, 다른 순서
		long seed = 123456789L;

		assertThat(Lottery.draw(a, 2, seed)).isEqualTo(Lottery.draw(b, 2, seed));
	}

	@Test
	@DisplayName("winnerCount가 후보보다 크면 전원 당첨")
	void everyone_wins_when_count_exceeds_candidates() {
		assertThat(Lottery.draw(List.of(1L, 2L, 3L), 10, 42L)).containsExactlyInAnyOrder(1L, 2L, 3L);
	}

	@Test
	@DisplayName("공정성 분포 — 후보 10명·당첨 1명·10,000회, 각 후보 ~1000회 (±15%)")
	void fair_distribution() {
		List<Long> candidates = new ArrayList<>();
		for (long i = 1; i <= 10; i++) {
			candidates.add(i);
		}
		Map<Long, Integer> wins = new HashMap<>();
		Random seedGen = new Random(0); // 시드 생성기도 고정해 테스트 자체를 재현 가능하게
		int rounds = 10_000;
		for (int i = 0; i < rounds; i++) {
			Long winner = Lottery.draw(candidates, 1, seedGen.nextLong()).get(0);
			wins.merge(winner, 1, Integer::sum);
		}
		int min = wins.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
		int max = wins.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
		log.info("[추첨 분포] 후보 10 · 당첨 1 · {}회 → 후보별 당첨 min={} max={} (기대 {})",
				rounds, min, max, rounds / 10);

		assertThat(wins).hasSize(10);
		assertThat(min).isGreaterThan((int) (rounds / 10 * 0.85));
		assertThat(max).isLessThan((int) (rounds / 10 * 1.15));
	}
}
