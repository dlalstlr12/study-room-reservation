package com.studyroom.common.realtime;

import com.studyroom.common.cache.RoomScheduleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 룸 현황이 바뀌었을 때 부르는 단일 진입점. 예약·홀딩·만료 등 현황을 바꾸는 모든 지점이 여기로 모인다.
 *
 * <ol>
 *   <li>룸 현황 캐시 무효화 (다음 조회가 최신을 읽도록)</li>
 *   <li>{@code /topic/rooms/{roomId}} 로 {@link RoomChangeEvent} 브로드캐스트 (구독자 즉시 갱신)</li>
 * </ol>
 *
 * 브로드캐스트 실패가 예약/홀딩 트랜잭션을 깨지 않도록 삼켜서 로그만 남긴다.
 */
@Component
public class RoomChangeNotifier {

	private static final Logger log = LoggerFactory.getLogger(RoomChangeNotifier.class);

	private final RoomScheduleCache roomScheduleCache;
	private final SimpMessagingTemplate messaging;

	public RoomChangeNotifier(RoomScheduleCache roomScheduleCache, SimpMessagingTemplate messaging) {
		this.roomScheduleCache = roomScheduleCache;
		this.messaging = messaging;
	}

	public void roomChanged(Long roomId, Long actorMemberId) {
		roomScheduleCache.evictAll();
		try {
			messaging.convertAndSend("/topic/rooms/" + roomId,
					RoomChangeEvent.now(roomId, actorMemberId));
		} catch (RuntimeException e) {
			log.warn("룸 변경 브로드캐스트 실패: room={}", roomId, e);
		}
	}
}
