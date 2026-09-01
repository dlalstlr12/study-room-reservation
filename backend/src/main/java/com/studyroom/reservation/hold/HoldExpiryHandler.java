package com.studyroom.reservation.hold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 홀딩이 사라졌을 때(TTL 만료 또는 백스톱 정리) 실행할 후속 처리를 모은 곳.
 * 지금은 로깅만, 캐싱 도입(로드맵 3단계 후반) 시 룸 현황 캐시 무효화가 여기에 붙는다.
 */
@Component
public class HoldExpiryHandler {

	private static final Logger log = LoggerFactory.getLogger(HoldExpiryHandler.class);

	public void onHoldExpired(Long roomId) {
		log.debug("홀딩 만료 후속 처리: room={}", roomId);
	}
}
