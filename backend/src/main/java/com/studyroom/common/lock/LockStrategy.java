package com.studyroom.common.lock;

/**
 * 예약 생성 시 동시성 제어 방식. {@code reservation.lock.strategy} 로 선택한다.
 *
 * <ul>
 *   <li>{@code NONE} — 제어 없음. 오버부킹 재현/비교 기준.</li>
 *   <li>{@code PESSIMISTIC} — 룸 행 비관적 락(SELECT ... FOR UPDATE).</li>
 *   <li>{@code DISTRIBUTED} — Redisson 분산 락.</li>
 * </ul>
 */
public enum LockStrategy {
	NONE,
	PESSIMISTIC,
	DISTRIBUTED
}
