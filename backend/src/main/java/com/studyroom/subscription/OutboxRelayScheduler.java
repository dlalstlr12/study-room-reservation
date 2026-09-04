package com.studyroom.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 아웃박스 릴레이 주기 실행. 테스트는 {@code subscription.outbox.scheduler-enabled=false} 로 끄고
 * {@link OutboxRelay#relay()} 를 직접 부른다.
 */
@Component
@ConditionalOnProperty(prefix = "subscription.outbox", name = "scheduler-enabled",
		havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

	private final OutboxRelay outboxRelay;

	public OutboxRelayScheduler(OutboxRelay outboxRelay) {
		this.outboxRelay = outboxRelay;
	}

	@Scheduled(fixedDelayString = "${subscription.outbox.poll-ms:2000}",
			initialDelayString = "${subscription.outbox.poll-ms:2000}")
	public void poll() {
		try {
			outboxRelay.relay();
		} catch (RuntimeException e) {
			log.warn("[아웃박스] 릴레이 실패 — 다음 폴에서 재시도", e);
		}
	}
}
