package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.ranking.RankingRepository.MemberScore;
import com.studyroom.support.RankingScenarioSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Redis Sorted Set 래퍼 동작. */
class RankingRepositoryTest extends RankingScenarioSupport {

	@Test
	@DisplayName("add 는 점수를 누적하고 topN 은 내림차순으로 준다")
	void accumulates_and_orders() {
		rankingRepository.add(RankingScope.ALL, null, 1L, 30);
		rankingRepository.add(RankingScope.ALL, null, 1L, 20);   // 누적 50
		rankingRepository.add(RankingScope.ALL, null, 2L, 90);
		rankingRepository.add(RankingScope.ALL, null, 3L, 10);

		List<MemberScore> top = rankingRepository.topN(RankingScope.ALL, null, 2);

		assertThat(top).extracting(MemberScore::memberId).containsExactly(2L, 1L);
		assertThat(top).extracting(MemberScore::minutes).containsExactly(90L, 50L);
	}

	@Test
	@DisplayName("rankOf 는 1-based, 없으면 null")
	void rank_of() {
		rankingRepository.add(RankingScope.ALL, null, 1L, 100);
		rankingRepository.add(RankingScope.ALL, null, 2L, 50);

		assertThat(rankingRepository.rankOf(RankingScope.ALL, null, 1L)).isEqualTo(1);
		assertThat(rankingRepository.rankOf(RankingScope.ALL, null, 2L)).isEqualTo(2);
		assertThat(rankingRepository.rankOf(RankingScope.ALL, null, 999L)).isNull();
		assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, 999L)).isZero();
	}

	@Test
	@DisplayName("일간 키는 TTL 이 걸린다")
	void daily_key_has_ttl() {
		LocalDate today = LocalDate.now();
		rankingRepository.add(RankingScope.DAILY, today, 1L, 40);

		Long ttl = redis.getExpire(RankingRepository.DAILY_PREFIX + today);
		assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(48 * 3600);
	}
}
