package com.studyroom.reservation.schedule;

import com.studyroom.common.config.CacheConfig;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.hold.Hold;
import com.studyroom.reservation.hold.HoldRepository;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.service.RoomService;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룸 현황을 DB·Redis에서 읽어 <b>뷰어와 무관한</b> 형태로 캐싱한다.
 * {@code mine} 플래그는 {@link RoomScheduleService}가 캐시 밖에서 계산해 캐시가 사용자별로
 * 쪼개지지 않게 한다.
 */
@Component
public class RoomScheduleLoader {

	private final RoomService roomService;
	private final ReservationRepository reservationRepository;
	private final HoldRepository holdRepository;

	public RoomScheduleLoader(RoomService roomService, ReservationRepository reservationRepository,
			HoldRepository holdRepository) {
		this.roomService = roomService;
		this.reservationRepository = reservationRepository;
		this.holdRepository = holdRepository;
	}

	@Cacheable(cacheNames = CacheConfig.ROOM_SCHEDULE, key = "#roomId + ':' + #date")
	@Transactional(readOnly = true)
	public CachedSchedule load(Long roomId, LocalDate date) {
		Room room = roomService.getEntity(roomId);
		LocalDateTime dayStart = date.atStartOfDay();
		LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

		List<CachedEntry> entries = new ArrayList<>();
		for (Reservation r : reservationRepository.findRoomReservationsOverlapping(roomId, dayStart, dayEnd)) {
			entries.add(new CachedEntry(ScheduleEntryType.RESERVED, r.getStartAt(), r.getEndAt(),
					r.getMember().getId()));
		}
		for (Hold h : holdRepository.findByRoom(roomId)) {
			if (h.startAt().isBefore(dayEnd) && h.endAt().isAfter(dayStart)) {
				entries.add(new CachedEntry(ScheduleEntryType.HOLDING, h.startAt(), h.endAt(),
						h.memberId()));
			}
		}
		entries.sort(Comparator.comparing(CachedEntry::startAt));
		return new CachedSchedule(room.getName(), entries);
	}

	public record CachedSchedule(String roomName, List<CachedEntry> entries) implements Serializable {
	}

	public record CachedEntry(ScheduleEntryType type, LocalDateTime startAt, LocalDateTime endAt,
			Long ownerMemberId) implements Serializable {
	}
}
