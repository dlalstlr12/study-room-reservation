package com.studyroom.ranking;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;

/** 랭킹 범위. 각각 별도의 Redis Sorted Set 에 매핑된다. */
public enum RankingScope {

	/** 전체 누적 — {@code ranking:all} */
	ALL,
	/** 일간 — {@code ranking:daily:{yyyy-MM-dd}}, TTL 48h 로 자연 만료 */
	DAILY;

	/** {@code "all"} / {@code "daily"} (대소문자 무관) → enum. */
	public static RankingScope from(String value) {
		try {
			return valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "scope 는 all 또는 daily 여야 합니다.");
		}
	}
}
