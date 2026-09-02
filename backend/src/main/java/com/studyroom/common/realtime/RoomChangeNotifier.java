package com.studyroom.common.realtime;

import com.studyroom.common.cache.RoomScheduleCache;
import org.springframework.stereotype.Component;

/**
 * 룸 현황이 바뀌었을 때 부르는 단일 진입점. 예약·홀딩·만료 등 현황을 바꾸는 모든 지점이 여기로 모인다.
 *
 * <p>지금은 캐시 무효화만. 로드맵 4단계에서 WebSocket 브로드캐스트가 여기에 붙는다.
 *
 * @param actorMemberId 변경을 일으킨 회원 (만료·백스톱은 {@code null})
 */
@Component
public class RoomChangeNotifier {

	private final RoomScheduleCache roomScheduleCache;

	public RoomChangeNotifier(RoomScheduleCache roomScheduleCache) {
		this.roomScheduleCache = roomScheduleCache;
	}

	public void roomChanged(Long roomId, Long actorMemberId) {
		roomScheduleCache.evictAll();
	}
}
