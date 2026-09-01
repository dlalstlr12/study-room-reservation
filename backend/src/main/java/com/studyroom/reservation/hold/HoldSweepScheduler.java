package com.studyroom.reservation.hold;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 백스톱 스윕. keyspace 만료 이벤트를 놓쳤을 때(앱 재시작 등) 인덱스와 캐시 정합성을 맞춘다.
 * 이벤트가 즉시성을, 이 스케줄러가 최종 정합성을 담당한다.
 */
@Component
public class HoldSweepScheduler {

	private static final Logger log = LoggerFactory.getLogger(HoldSweepScheduler.class);

	private final HoldRepository holdRepository;
	private final HoldExpiryHandler expiryHandler;

	public HoldSweepScheduler(HoldRepository holdRepository, HoldExpiryHandler expiryHandler) {
		this.holdRepository = holdRepository;
		this.expiryHandler = expiryHandler;
	}

	@Scheduled(fixedDelayString = "${reservation.hold.sweep-ms:60000}",
			initialDelayString = "${reservation.hold.sweep-ms:60000}")
	public void sweep() {
		Set<Long> affectedRooms = holdRepository.sweep();
		if (!affectedRooms.isEmpty()) {
			log.debug("백스톱 홀딩 정리: rooms={}", affectedRooms);
			affectedRooms.forEach(expiryHandler::onHoldExpired);
		}
	}
}
