package com.studyroom.reservation.hold;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.lock.DistributedLock;
import com.studyroom.common.lock.RedissonDistributedLock;
import com.studyroom.common.realtime.RoomChangeNotifier;
import com.studyroom.member.entity.Member;
import com.studyroom.reservation.SlotValidator;
import com.studyroom.reservation.dto.ReservationResponse;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.service.RoomService;
import com.studyroom.member.service.MemberService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 좌석 홀딩. 룸 선택 → <b>홀딩(TTL)</b> → 확정 흐름의 가운데 단계.
 *
 * <p>락으로도 "룸 선택 후 확정까지의 몇 분"은 잡아둘 수 없다(락 수명 = 트랜잭션).
 * 먼저 고른 사람에게 Redis TTL만큼 확정 유예를 주고, 나머지는 즉시 실패시킨다.
 *
 * <p>홀딩은 Redis 전용이므로 {@code reservation.lock.strategy}(2단계)와 무관하게
 * 항상 Redisson 락으로 룸 단위 임계 구역을 만든다.
 */
@Service
public class HoldService {

	private final HoldRepository holdRepository;
	private final ReservationRepository reservationRepository;
	private final RoomService roomService;
	private final MemberService memberService;
	private final TransactionTemplate txTemplate;
	private final HoldTtlPolicy holdTtlPolicy;
	private final RoomChangeNotifier roomChangeNotifier;
	private final DistributedLock roomLock;

	public HoldService(HoldRepository holdRepository, ReservationRepository reservationRepository,
			RoomService roomService, MemberService memberService, TransactionTemplate txTemplate,
			HoldTtlPolicy holdTtlPolicy, RoomChangeNotifier roomChangeNotifier,
			RedissonClient redissonClient) {
		this.holdRepository = holdRepository;
		this.reservationRepository = reservationRepository;
		this.roomService = roomService;
		this.memberService = memberService;
		this.txTemplate = txTemplate;
		this.holdTtlPolicy = holdTtlPolicy;
		this.roomChangeNotifier = roomChangeNotifier;
		this.roomLock = new RedissonDistributedLock(redissonClient);
	}

	public HoldResponse hold(Long memberId, HoldCreateRequest request) {
		SlotValidator.validate(request.startAt(), request.endAt());
		Room room = roomService.getEntity(request.roomId());

		Hold hold = roomLock.runWithLock(lockKey(room.getId()), () -> {
			if (reservationRepository.existsOverlap(room.getId(), request.startAt(), request.endAt())) {
				throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT);
			}
			boolean holdClash = holdRepository.findByRoom(room.getId()).stream()
					.anyMatch(h -> h.overlaps(request.startAt(), request.endAt()));
			if (holdClash) {
				throw new BusinessException(ErrorCode.RESERVATION_HOLD_CONFLICT);
			}
			Duration ttl = holdTtlPolicy.ttlFor(memberId);
			Hold created = Hold.create(room.getId(), memberId, request.startAt(), request.endAt(),
					LocalDateTime.now().plus(ttl));
			holdRepository.save(created, ttl);
			return created;
		});
		roomChangeNotifier.roomChanged(room.getId(), memberId);
		return HoldResponse.from(hold, room.getName());
	}

	public ReservationResponse confirm(Long memberId, Long roomId, String holdId) {
		ReservationResponse result = roomLock.runWithLock(lockKey(roomId), () -> {
			Hold hold = holdRepository.find(roomId, holdId)
					.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_HOLD_NOT_FOUND));
			if (!hold.ownedBy(memberId)) {
				throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
			}
			ReservationResponse reservation = txTemplate.execute(status -> {
				if (reservationRepository.existsOverlap(roomId, hold.startAt(), hold.endAt())) {
					throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT);
				}
				Member member = memberService.getById(memberId);
				Room room = roomService.getEntity(roomId);
				Reservation saved = reservationRepository.save(
						Reservation.create(member, room, hold.startAt(), hold.endAt()));
				return ReservationResponse.from(saved);
			});
			holdRepository.delete(hold);
			return reservation;
		});
		roomChangeNotifier.roomChanged(roomId, memberId);
		return result;
	}

	public void release(Long memberId, Long roomId, String holdId) {
		Hold hold = holdRepository.find(roomId, holdId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_HOLD_NOT_FOUND));
		if (!hold.ownedBy(memberId)) {
			throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
		}
		holdRepository.delete(hold);
		roomChangeNotifier.roomChanged(roomId, memberId);
	}

	public List<HoldResponse> myHolds(Long memberId) {
		List<Hold> holds = holdRepository.findByMember(memberId);
		if (holds.isEmpty()) {
			return List.of();
		}
		Map<Long, String> roomNames = roomService.getRoomNames(
				holds.stream().map(Hold::roomId).distinct().toList());
		return holds.stream()
				.map(h -> HoldResponse.from(h, roomNames.getOrDefault(h.roomId(), "(삭제된 룸)")))
				.toList();
	}

	private String lockKey(Long roomId) {
		return "lock:reservation:room:" + roomId;
	}
}
