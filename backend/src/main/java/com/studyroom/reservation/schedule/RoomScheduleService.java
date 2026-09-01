package com.studyroom.reservation.schedule;

import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.hold.Hold;
import com.studyroom.reservation.hold.HoldRepository;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.service.RoomService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룸별 하루 예약 현황. 현황판(프론트 타임라인)이 자주 조회하므로 캐싱 대상이다
 * (로드맵 3단계 후반에서 {@code @Cacheable} 부착).
 */
@Service
public class RoomScheduleService {

	private final RoomService roomService;
	private final ReservationRepository reservationRepository;
	private final HoldRepository holdRepository;

	public RoomScheduleService(RoomService roomService, ReservationRepository reservationRepository,
			HoldRepository holdRepository) {
		this.roomService = roomService;
		this.reservationRepository = reservationRepository;
		this.holdRepository = holdRepository;
	}

	@Transactional(readOnly = true)
	public RoomScheduleResponse getSchedule(Long roomId, LocalDate date, Long viewerMemberId) {
		Room room = roomService.getEntity(roomId);
		LocalDateTime dayStart = date.atStartOfDay();
		LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

		List<RoomScheduleResponse.Entry> entries = new ArrayList<>();

		for (Reservation r : reservationRepository.findRoomReservationsOverlapping(roomId, dayStart, dayEnd)) {
			entries.add(new RoomScheduleResponse.Entry(ScheduleEntryType.RESERVED,
					r.getStartAt(), r.getEndAt(),
					viewerMemberId != null && r.getMember().getId().equals(viewerMemberId)));
		}
		for (Hold h : holdRepository.findByRoom(roomId)) {
			if (h.startAt().isBefore(dayEnd) && h.endAt().isAfter(dayStart)) {
				entries.add(new RoomScheduleResponse.Entry(ScheduleEntryType.HOLDING,
						h.startAt(), h.endAt(),
						viewerMemberId != null && h.memberId().equals(viewerMemberId)));
			}
		}
		entries.sort(Comparator.comparing(RoomScheduleResponse.Entry::startAt));

		return new RoomScheduleResponse(roomId, room.getName(), date, entries);
	}
}
