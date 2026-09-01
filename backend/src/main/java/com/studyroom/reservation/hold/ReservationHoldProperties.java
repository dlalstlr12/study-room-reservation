package com.studyroom.reservation.hold;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 홀딩 설정. {@code reservation.hold.*}
 *
 * @param ttl     홀딩 유지 시간 (기본 10분). 이 시간 안에 확정하지 않으면 Redis가 키를 지운다.
 * @param sweepMs 백스톱 스케줄러 주기 밀리초 (기본 60초). keyspace 이벤트를 놓쳤을 때 정합성을 맞춘다.
 */
@ConfigurationProperties("reservation.hold")
public record ReservationHoldProperties(Duration ttl, long sweepMs) {

	public ReservationHoldProperties {
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			ttl = Duration.ofMinutes(10);
		}
		if (sweepMs <= 0) {
			sweepMs = 60_000L;
		}
	}
}
