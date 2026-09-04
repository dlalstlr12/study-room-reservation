package com.studyroom.ranking.dto;

/** 내 순위. 아직 랭크에 없으면 {@code rank} 는 null. */
public record MyRankResponse(
		Integer rank,
		long minutes
) {
}
