package com.studyroom.common.lock;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redisson {@code RLock} 기반 분산 락. 앱이 여러 인스턴스여도 같은 키는 하나만 임계 구역에 들어간다.
 *
 * <p>{@code waitTime} 안에 락을 못 잡으면 {@link ErrorCode#RESERVATION_LOCK_TIMEOUT}.
 * {@code leaseTime} 이 지나면 자동 해제되어 홀더 장애 시에도 데드락을 피한다.
 */
public class RedissonDistributedLock implements DistributedLock {

	private static final Logger log = LoggerFactory.getLogger(RedissonDistributedLock.class);
	private static final long WAIT_TIME_SEC = 3L;
	private static final long LEASE_TIME_SEC = 10L;

	private final RedissonClient redissonClient;

	public RedissonDistributedLock(RedissonClient redissonClient) {
		this.redissonClient = redissonClient;
	}

	@Override
	public <T> T runWithLock(String key, Supplier<T> action) {
		RLock lock = redissonClient.getLock(key);
		boolean acquired = false;
		try {
			acquired = lock.tryLock(WAIT_TIME_SEC, LEASE_TIME_SEC, TimeUnit.SECONDS);
			if (!acquired) {
				throw new BusinessException(ErrorCode.RESERVATION_LOCK_TIMEOUT);
			}
			return action.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.RESERVATION_LOCK_TIMEOUT);
		} finally {
			if (acquired && lock.isHeldByCurrentThread()) {
				try {
					lock.unlock();
				} catch (IllegalMonitorStateException e) {
					log.warn("이미 해제된 락 unlock 시도: {}", key);
				}
			}
		}
	}
}
