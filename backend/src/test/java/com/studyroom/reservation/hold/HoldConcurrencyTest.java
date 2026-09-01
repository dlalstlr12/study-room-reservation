package com.studyroom.reservation.hold;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.support.HoldScenarioSupport;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 여러 명이 같은 룸·같은 슬롯을 동시에 홀딩해도 한 명만 성공한다.
 * 락으로도 못 막던 "확정 단계 몰림"을 홀딩이 빠른 실패로 바꾼다.
 */
class HoldConcurrencyTest extends HoldScenarioSupport {

	private static final int THREADS = 30;

	@Test
	@DisplayName("동시 홀딩 30건 중 1건만 성공, 나머지는 HOLD_CONFLICT")
	void only_one_hold_wins() throws InterruptedException {
		long roomId = newRoom().getId();

		AtomicInteger success = new AtomicInteger();
		AtomicInteger holdConflict = new AtomicInteger();
		AtomicInteger other = new AtomicInteger();
		AtomicReference<String> winningHoldId = new AtomicReference<>();

		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch go = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(THREADS);

		for (int i = 0; i < THREADS; i++) {
			Long memberId = newMember();
			pool.submit(() -> {
				ready.countDown();
				try {
					go.await();
					HoldResponse held = holdService.hold(memberId, holdRequest(roomId, 10, 1));
					success.incrementAndGet();
					winningHoldId.set(held.holdId());
				} catch (BusinessException e) {
					if (e.getErrorCode() == ErrorCode.RESERVATION_HOLD_CONFLICT
							|| e.getErrorCode() == ErrorCode.RESERVATION_LOCK_TIMEOUT) {
						holdConflict.incrementAndGet();
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

		ready.await();
		go.countDown();
		done.await();
		pool.shutdownNow();

		assertThat(success.get()).isEqualTo(1);
		assertThat(other.get()).isZero();
		assertThat(holdConflict.get()).isEqualTo(THREADS - 1);
		assertThat(holdRepository.findByRoom(roomId)).hasSize(1);

		// 승자가 확정하면 예약 1건
		Long winner = holdRepository.find(roomId, winningHoldId.get()).orElseThrow().memberId();
		holdService.confirm(winner, roomId, winningHoldId.get());
		assertThat(reservationRepository.countByRoomIdAndStatus(roomId, ReservationStatus.RESERVED))
				.isEqualTo(1);
	}
}
