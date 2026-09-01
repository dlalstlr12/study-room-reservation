package com.studyroom.common.config;

import com.studyroom.common.lock.DistributedLock;
import com.studyroom.common.lock.NoOpDistributedLock;
import com.studyroom.common.lock.ReservationLockProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 전략({@code reservation.lock.strategy})에 맞는 {@link DistributedLock} 구현을 고른다.
 */
@Configuration
public class LockConfig {

	@Bean
	public DistributedLock distributedLock(ReservationLockProperties properties) {
		// DISTRIBUTED 분기는 후속 커밋(Redisson 분산 락)에서 추가한다.
		return new NoOpDistributedLock();
	}
}
