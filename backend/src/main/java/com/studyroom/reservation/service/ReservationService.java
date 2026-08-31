package com.studyroom.reservation.service;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.entity.Member;
import com.studyroom.member.service.MemberService;
import com.studyroom.reservation.dto.ReservationCreateRequest;
import com.studyroom.reservation.dto.ReservationResponse;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.service.RoomService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

	/** 1회 예약 최대 이용 시간. */
	private static final Duration MAX_DURATION = Duration.ofHours(4);

	private final ReservationRepository reservationRepository;
	private final MemberService memberService;
	private final RoomService roomService;

	public ReservationService(ReservationRepository reservationRepository,
			MemberService memberService, RoomService roomService) {
		this.reservationRepository = reservationRepository;
		this.memberService = memberService;
		this.roomService = roomService;
	}

	/**
	 * 예약 생성. <b>동시성 미처리</b> — 겹침 검사({@code existsOverlap})와 저장 사이에 락이 없어
	 * 두 요청이 동시에 검사를 통과하면 겹치는 예약이 함께 생성될 수 있다.
	 * 로드맵 2단계에서 "버그 재현 → 비관적 락 → Redisson 분산 락" 순으로 해결한다. 의도적 취약.
	 */
	@Transactional
	public ReservationResponse create(Long memberId, ReservationCreateRequest request) {
		validateTimeRange(request.startAt(), request.endAt());

		Member member = memberService.getById(memberId);
		Room room = roomService.getEntity(request.roomId());

		if (reservationRepository.existsOverlap(room.getId(), request.startAt(), request.endAt())) {
			throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT);
		}

		Reservation saved = reservationRepository.save(
				Reservation.create(member, room, request.startAt(), request.endAt()));
		return ReservationResponse.from(saved);
	}

	public List<ReservationResponse> getMyReservations(Long memberId, ReservationStatus status) {
		List<Reservation> reservations = (status == null)
				? reservationRepository.findByMemberIdOrderByStartAtDesc(memberId)
				: reservationRepository.findByMemberIdAndStatusOrderByStartAtDesc(memberId, status);
		return reservations.stream().map(ReservationResponse::from).toList();
	}

	public ReservationResponse getReservation(Long reservationId, Long requesterId, boolean isAdmin) {
		Reservation reservation = reservationRepository.findDetailById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
		if (!isAdmin && !reservation.isOwnedBy(requesterId)) {
			throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
		}
		return ReservationResponse.from(reservation);
	}

	@Transactional
	public ReservationResponse cancel(Long reservationId, Long requesterId) {
		Reservation reservation = reservationRepository.findDetailById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
		if (!reservation.isOwnedBy(requesterId)) {
			throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
		}
		reservation.cancel();
		return ReservationResponse.from(reservation);
	}

	private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
		if (!startAt.isBefore(endAt)) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME, "시작 시각은 종료 시각보다 앞서야 합니다.");
		}
		if (Duration.between(startAt, endAt).compareTo(MAX_DURATION) > 0) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME,
					"1회 예약은 최대 " + MAX_DURATION.toHours() + "시간까지 가능합니다.");
		}
	}
}
