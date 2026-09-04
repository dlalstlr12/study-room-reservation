package com.studyroom.ranking;

/** 랭킹 범위. 각각 별도의 Redis Sorted Set 에 매핑된다. */
public enum RankingScope {

	/** 전체 누적 — {@code ranking:all} */
	ALL,
	/** 일간 — {@code ranking:daily:{yyyy-MM-dd}}, TTL 48h 로 자연 만료 */
	DAILY
}
