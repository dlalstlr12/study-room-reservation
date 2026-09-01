package com.studyroom.common.config;

import com.studyroom.common.lock.DistributedLock;
import com.studyroom.common.lock.LockStrategy;
import com.studyroom.common.lock.NoOpDistributedLock;
import com.studyroom.common.lock.RedissonDistributedLock;
import com.studyroom.common.lock.ReservationLockProperties;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 전략({@code reservation.lock.strategy})에 맞는 {@link DistributedLock} 구현을 고른다.
 * {@code DISTRIBUTED} 만 Redisson 을 쓰고, 나머지는 무동작(NONE) 또는 DB 행 락(PESSIMISTIC).
 */
@Configuration
public class LockConfig {

	@Bean
	public DistributedLock distributedLock(ReservationLockProperties properties,
			RedissonClient redissonClient) {
		if (properties.strategy() == LockStrategy.DISTRIBUTED) {
			return new RedissonDistributedLock(redissonClient);
		}
		return new NoOpDistributedLock();
	}
}
