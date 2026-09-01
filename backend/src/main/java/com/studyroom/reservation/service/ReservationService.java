package com.studyroom.reservation.service;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.lock.DistributedLock;
import com.studyroom.common.lock.LockStrategy;
import com.studyroom.common.lock.ReservationLockProperties;
import com.studyroom.member.entity.Member;
import com.studyroom.member.service.MemberService;
import com.studyroom.reservation.SlotValidator;
import com.studyroom.reservation.dto.ReservationCreateRequest;
import com.studyroom.reservation.dto.ReservationResponse;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import com.studyroom.room.service.RoomService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final MemberService memberService;
	private final RoomService roomService;
	private final RoomRepository roomRepository;
	private final TransactionTemplate txTemplate;
	private final DistributedLock distributedLock;
	private final ReservationLockProperties lockProperties;

	public ReservationService(ReservationRepository reservationRepository, MemberService memberService,
			RoomService roomService, RoomRepository roomRepository, TransactionTemplate txTemplate,
			DistributedLock distributedLock, ReservationLockProperties lockProperties) {
		this.reservationRepository = reservationRepository;
		this.memberService = memberService;
		this.roomService = roomService;
		this.roomRepository = roomRepository;
		this.txTemplate = txTemplate;
		this.distributedLock = distributedLock;
		this.lockProperties = lockProperties;
	}

	/**
	 * 예약 생성. 순서를 <b>락 → 트랜잭션 → 겹침 검사 → 저장</b>으로 명시한다.
	 * 검증은 트랜잭션 밖에서, 겹침 검사와 저장은 한 트랜잭션 안에서 이뤄진다.
	 * 동시성 제어 방식은 {@code reservation.lock.strategy}로 전환한다
	 * (자세한 배경: {@code docs/troubleshooting.md}).
	 */
	public ReservationResponse create(Long memberId, ReservationCreateRequest request) {
		SlotValidator.validate(request.startAt(), request.endAt());

		String lockKey = "lock:reservation:room:" + request.roomId();
		return distributedLock.runWithLock(lockKey,
				() -> txTemplate.execute(status -> doCreate(memberId, request)));
	}

	private ReservationResponse doCreate(Long memberId, ReservationCreateRequest request) {
		if (lockProperties.strategy() == LockStrategy.PESSIMISTIC) {
			// 룸 행에 FOR UPDATE — 같은 룸의 예약 생성이 이 트랜잭션 뒤로 줄 선다.
			roomRepository.findByIdForUpdate(request.roomId())
					.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
		}

		Member member = memberService.getById(memberId);
		Room room = roomService.getEntity(request.roomId());

		if (reservationRepository.existsOverlap(room.getId(), request.startAt(), request.endAt())) {
			throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT);
		}

		Reservation saved = reservationRepository.save(
				Reservation.create(member, room, request.startAt(), request.endAt()));
		return ReservationResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(Long memberId, ReservationStatus status) {
		List<Reservation> reservations = (status == null)
				? reservationRepository.findByMemberIdOrderByStartAtDesc(memberId)
				: reservationRepository.findByMemberIdAndStatusOrderByStartAtDesc(memberId, status);
		return reservations.stream().map(ReservationResponse::from).toList();
	}

	@Transactional(readOnly = true)
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
}
