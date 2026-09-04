package com.studyroom.reservation.hold;

import java.time.Duration;

/**
 * 회원별 홀딩 유예 시간. 구독 도메인이 구현해 PRO 구독자에게 더 긴 유예를 준다
 * (도메인 간 연계 — 예약은 구독을 직접 알지 않고 이 포트만 안다).
 */
public interface HoldTtlPolicy {

	Duration ttlFor(Long memberId);
}
