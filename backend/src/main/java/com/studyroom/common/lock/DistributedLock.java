package com.studyroom.common.lock;

import java.util.function.Supplier;

/**
 * 이름 붙은 락 아래에서 작업을 실행한다. 구현체는 전략에 따라 주입된다
 * ({@link NoOpDistributedLock} 또는 Redisson 기반).
 */
public interface DistributedLock {

	<T> T runWithLock(String key, Supplier<T> action);
}
