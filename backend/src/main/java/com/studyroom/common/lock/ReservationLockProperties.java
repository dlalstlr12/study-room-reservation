package com.studyroom.common.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code reservation.lock.*} 바인딩.
 *
 * @param strategy 동시성 제어 방식 (기본 {@link LockStrategy#NONE})
 */
@ConfigurationProperties(prefix = "reservation.lock")
public record ReservationLockProperties(LockStrategy strategy) {

	public ReservationLockProperties {
		if (strategy == null) {
			strategy = LockStrategy.NONE;
		}
	}
}
