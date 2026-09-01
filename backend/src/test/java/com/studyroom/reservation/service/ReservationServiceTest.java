package com.studyroom.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.lock.LockStrategy;
import com.studyroom.common.lock.NoOpDistributedLock;
import com.studyroom.common.lock.ReservationLockProperties;
import com.studyroom.member.entity.Member;
import com.studyroom.member.service.MemberService;
import com.studyroom.reservation.dto.ReservationCreateRequest;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import com.studyroom.room.service.RoomService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;
	@Mock
	private MemberService memberService;
	@Mock
	private RoomService roomService;
	@Mock
	private RoomRepository roomRepository;
	@Mock
	private TransactionTemplate txTemplate;

	private ReservationService reservationService;

	private Member member;
	private Room room;

	@BeforeEach
	void setUp() {
		reservationService = new ReservationService(reservationRepository, memberService, roomService,
				roomRepository, txTemplate, new NoOpDistributedLock(),
				new ReservationLockProperties(LockStrategy.NONE));
		// txTemplate.execute 는 콜백을 그대로 실행한다 (트랜잭션 경계는 통합 테스트에서 검증).
		lenient().when(txTemplate.execute(any())).thenAnswer(
				inv -> inv.getArgument(0, TransactionCallback.class).doInTransaction(null));

		member = Member.create("user@test.com", "ENCODED", "테스터");
		ReflectionTestUtils.setField(member, "id", 1L);
		room = Room.create("스터디룸", 4, null);
		ReflectionTestUtils.setField(room, "id", 10L);
	}

	private ReservationCreateRequest request(LocalDateTime start, LocalDateTime end) {
		return new ReservationCreateRequest(10L, start, end);
	}

	@Test
	void 겹치지_않으면_예약이_생성된다() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		when(memberService.getById(1L)).thenReturn(member);
		when(roomService.getEntity(10L)).thenReturn(room);
		when(reservationRepository.existsOverlap(eq(10L), any(), any())).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

		var response = reservationService.create(1L, request(start, start.plusHours(2)));

		assertThat(response.roomId()).isEqualTo(10L);
		assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void 시간이_겹치면_RESERVATION_TIME_CONFLICT() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);
		when(memberService.getById(1L)).thenReturn(member);
		when(roomService.getEntity(10L)).thenReturn(room);
		when(reservationRepository.existsOverlap(eq(10L), any(), any())).thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(1L, request(start, start.plusHours(2))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_TIME_CONFLICT);
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void 시작이_종료보다_늦으면_INVALID_RESERVATION_TIME() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);

		assertThatThrownBy(() -> reservationService.create(1L, request(start, start.minusHours(1))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_RESERVATION_TIME);
	}

	@Test
	void 최대_이용시간을_초과하면_INVALID_RESERVATION_TIME() {
		LocalDateTime start = LocalDateTime.now().plusDays(1);

		assertThatThrownBy(() -> reservationService.create(1L, request(start, start.plusHours(5))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_RESERVATION_TIME);
	}

	@Test
	void 비소유자가_취소하면_RESERVATION_ACCESS_DENIED() {
		Reservation reservation = Reservation.create(member, room,
				LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
		ReflectionTestUtils.setField(reservation, "id", 100L);
		when(reservationRepository.findDetailById(100L)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.cancel(100L, 999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED);
	}

	@Test
	void 소유자가_취소하면_상태가_CANCELLED가_된다() {
		Reservation reservation = Reservation.create(member, room,
				LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
		ReflectionTestUtils.setField(reservation, "id", 100L);
		when(reservationRepository.findDetailById(100L)).thenReturn(Optional.of(reservation));

		var response = reservationService.cancel(100L, 1L);

		assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
	}

	@Test
	void 상세조회는_소유자가_아니고_관리자도_아니면_거부된다() {
		Reservation reservation = Reservation.create(member, room,
				LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
		ReflectionTestUtils.setField(reservation, "id", 100L);
		when(reservationRepository.findDetailById(100L)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.getReservation(100L, 999L, false))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED);

		assertThat(reservationService.getReservation(100L, 999L, true).id()).isEqualTo(100L);
	}
}
