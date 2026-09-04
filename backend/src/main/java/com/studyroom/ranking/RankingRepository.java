package com.studyroom.ranking;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

/**
 * 랭킹 Sorted Set 접근. 점수는 누적 이용 분이다.
 *
 * <ul>
 *   <li>갱신 {@code ZINCRBY} — Redis 단일 명령이라 <b>그 자체가 원자적</b>. 여러 워커·스레드가
 *       같은 멤버를 동시에 올려도 정확하다 (read-modify-write 아님).</li>
 *   <li>조회 {@code ZREVRANGE} — O(log N + M). DB {@code GROUP BY ... ORDER BY} 없이 상위권.</li>
 * </ul>
 */
@Repository
public class RankingRepository {

	static final String ALL_KEY = "ranking:all";
	static final String DAILY_PREFIX = "ranking:daily:";
	private static final Duration DAILY_TTL = Duration.ofHours(48);

	private final StringRedisTemplate redis;

	public RankingRepository(StringRedisTemplate redis) {
		this.redis = redis;
	}

	static String key(RankingScope scope, LocalDate date) {
		if (scope == RankingScope.ALL) {
			return ALL_KEY;
		}
		if (date == null) {
			throw new IllegalArgumentException("일간 랭킹은 날짜가 필요합니다.");
		}
		return DAILY_PREFIX + date;
	}

	/** {@code deltaMinutes} 만큼 점수를 올린다. 일간 키는 TTL을 갱신한다. */
	public void add(RankingScope scope, LocalDate date, Long memberId, long deltaMinutes) {
		String key = key(scope, date);
		redis.opsForZSet().incrementScore(key, memberId.toString(), deltaMinutes);
		if (scope == RankingScope.DAILY) {
			redis.expire(key, DAILY_TTL);
		}
	}

	/** 상위 {@code n} 명 (점수 내림차순). */
	public List<MemberScore> topN(RankingScope scope, LocalDate date, int n) {
		Set<TypedTuple<String>> tuples = redis.opsForZSet()
				.reverseRangeWithScores(key(scope, date), 0, n - 1L);
		if (tuples == null) {
			return List.of();
		}
		return tuples.stream()
				.map(t -> new MemberScore(Long.parseLong(t.getValue()),
						t.getScore() == null ? 0L : t.getScore().longValue()))
				.toList();
	}

	/** 1-based 순위. 랭크에 없으면 null. */
	public Integer rankOf(RankingScope scope, LocalDate date, Long memberId) {
		Long rank = redis.opsForZSet().reverseRank(key(scope, date), memberId.toString());
		return rank == null ? null : rank.intValue() + 1;
	}

	public long scoreOf(RankingScope scope, LocalDate date, Long memberId) {
		Double score = redis.opsForZSet().score(key(scope, date), memberId.toString());
		return score == null ? 0L : score.longValue();
	}

	public void clear(RankingScope scope, LocalDate date) {
		redis.delete(key(scope, date));
	}

	public record MemberScore(Long memberId, long minutes) {
	}
}
