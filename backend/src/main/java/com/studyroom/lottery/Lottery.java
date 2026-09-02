package com.studyroom.lottery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 재현 가능한 추첨. 후보를 <b>memberId 오름차순</b>으로 정렬해 결정적 기준 순서를 만든 뒤
 * {@code new Random(seed)} 로 섞어 앞에서 {@code winnerCount} 명을 뽑는다.
 *
 * <p>같은 (후보 집합, seed, winnerCount) 면 언제 어디서 실행해도 같은 당첨자가 나온다.
 * 분쟁 시 저장된 seed 로 재실행해 검증할 수 있다.
 */
public final class Lottery {

	private Lottery() {
	}

	/** @return 당첨된 memberId 목록 (입력 후보 수보다 winnerCount 가 크면 전원 당첨) */
	public static List<Long> draw(List<Long> candidateMemberIds, int winnerCount, long seed) {
		List<Long> ordered = new ArrayList<>(candidateMemberIds);
		Collections.sort(ordered);
		Collections.shuffle(ordered, new Random(seed));
		return List.copyOf(ordered.subList(0, Math.min(winnerCount, ordered.size())));
	}
}
