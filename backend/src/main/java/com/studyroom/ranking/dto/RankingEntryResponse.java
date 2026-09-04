package com.studyroom.ranking.dto;

/** 랭킹 한 줄. */
public record RankingEntryResponse(
		int rank,
		Long memberId,
		String memberName,
		long minutes
) {
}
