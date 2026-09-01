package com.studyroom.common.lock;

import java.util.function.Supplier;

/**
 * 아무 락도 잡지 않는다. {@code NONE}·{@code PESSIMISTIC} 전략에서 주입된다
 * (PESSIMISTIC 은 DB 행 락을 트랜잭션 안에서 별도로 잡는다).
 */
public class NoOpDistributedLock implements DistributedLock {

	@Override
	public <T> T runWithLock(String key, Supplier<T> action) {
		return action.get();
	}
}
