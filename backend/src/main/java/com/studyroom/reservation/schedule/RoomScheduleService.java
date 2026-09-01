package com.studyroom.reservation.schedule;

import com.studyroom.reservation.schedule.RoomScheduleLoader.CachedSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 룸별 하루 예약 현황. 캐시된 뷰어-무관 데이터에 요청자 기준 {@code mine} 플래그를 입힌다.
 */
@Service
public class RoomScheduleService {

	private final RoomScheduleLoader loader;

	public RoomScheduleService(RoomScheduleLoader loader) {
		this.loader = loader;
	}

	public RoomScheduleResponse getSchedule(Long roomId, LocalDate date, Long viewerMemberId) {
		CachedSchedule cached = loader.load(roomId, date);
		List<RoomScheduleResponse.Entry> entries = cached.entries().stream()
				.map(e -> new RoomScheduleResponse.Entry(e.type(), e.startAt(), e.endAt(),
						viewerMemberId != null && viewerMemberId.equals(e.ownerMemberId())))
				.toList();
		return new RoomScheduleResponse(roomId, cached.roomName(), date, entries);
	}
}
