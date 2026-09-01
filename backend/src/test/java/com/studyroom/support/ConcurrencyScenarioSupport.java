package com.studyroom.support;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.reservation.dto.ReservationCreateRequest;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.reservation.service.ReservationService;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 같은 룸·같은 시간대로 동시에 예약을 생성하는 시나리오. 락 전략별 테스트가 공유한다.
 * 실제 컨테이너 DB에 커밋되므로 각 테스트는 고유한 룸/회원을 새로 만들고 그 룸 기준으로만 검증한다.
 */
public abstract class ConcurrencyScenarioSupport extends IntegrationTest {

	@Autowired
	protected ReservationService reservationService;
	@Autowired
	protected ReservationRepository reservationRepository;
	@Autowired
	protected RoomRepository roomRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;

	protected record Result(int success, int conflict, int other) {
	}

	protected Room newRoom() {
		return roomRepository.save(Room.create("동시성-테스트-" + UUID.randomUUID(), 4, null));
	}

	protected List<Long> newMembers(int count) {
		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Member m = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원" + i);
			ids.add(memberRepository.save(m).getId());
		}
		return ids;
	}

	/**
	 * {@code threads}개 스레드가 배리어에서 동시에 출발해 같은 룸·같은 구간으로 예약을 생성한다.
	 */
	protected Result runConcurrentCreates(long roomId, List<Long> memberIds, int threads) {
		// 30분 슬롯에 정렬 (SlotValidator 규칙)
		LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS);
		ReservationCreateRequest request = new ReservationCreateRequest(roomId, start, start.plusHours(1));

		AtomicInteger success = new AtomicInteger();
		AtomicInteger conflict = new AtomicInteger();
		AtomicInteger other = new AtomicInteger();

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch go = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			long memberId = memberIds.get(i % memberIds.size());
			pool.submit(() -> {
				ready.countDown();
				try {
					go.await();
					reservationService.create(memberId, request);
					success.incrementAndGet();
				} catch (BusinessException e) {
					if (e.getErrorCode() == ErrorCode.RESERVATION_TIME_CONFLICT
							|| e.getErrorCode() == ErrorCode.RESERVATION_LOCK_TIMEOUT) {
						conflict.incrementAndGet();
					} else {
						other.incrementAndGet();
					}
				} catch (Exception e) {
					other.incrementAndGet();
				} finally {
					done.countDown();
				}
			});
		}

		try {
			ready.await();
			go.countDown();
			done.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		} finally {
			pool.shutdownNow();
		}
		return new Result(success.get(), conflict.get(), other.get());
	}

	protected long reservedCount(long roomId) {
		return reservationRepository.countByRoomIdAndStatus(roomId, ReservationStatus.RESERVED);
	}
}
