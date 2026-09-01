package com.studyroom.reservation.hold;

import com.studyroom.common.cache.RoomScheduleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 홀딩이 사라졌을 때(TTL 만료 또는 백스톱 정리) 실행할 후속 처리를 모은 곳.
 * 룸 현황 캐시를 비워 만료된 홀딩이 현황판에서 곧바로 사라지게 한다.
 */
@Component
public class HoldExpiryHandler {

	private static final Logger log = LoggerFactory.getLogger(HoldExpiryHandler.class);

	private final RoomScheduleCache roomScheduleCache;

	public HoldExpiryHandler(RoomScheduleCache roomScheduleCache) {
		this.roomScheduleCache = roomScheduleCache;
	}

	public void onHoldExpired(Long roomId) {
		log.debug("홀딩 만료 후속 처리: room={}", roomId);
		roomScheduleCache.evictAll();
	}
}
