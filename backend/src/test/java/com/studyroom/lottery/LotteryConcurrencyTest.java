package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.support.LotteryScenarioSupport;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스케줄러 중복 실행이나 다중 인스턴스에서 같은 이벤트를 동시에 추첨해도 딱 한 번만 뽑힌다.
 */
class LotteryConcurrencyTest extends LotteryScenarioSupport {

	@Test
	@DisplayName("동시 추첨 — 한 번만 성공, 응모자 중복 생성 없음")
	void concurrent_draw_runs_once() throws InterruptedException {
		LocalDateTime target = tomorrowAt(11);
		int candidates = 6;
		for (int i = 0; i < candidates; i++) {
			reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		}
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"동시 추첨", "상품", target, LocalDateTime.now().plusHours(1), 2)).id();

		int threads = 8;
		AtomicInteger success = new AtomicInteger();
		AtomicInteger alreadyDrawn = new AtomicInteger();
		AtomicInteger other = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch go = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				ready.countDown();
				try {
					go.await();
					lotteryService.draw(eventId, true);
					success.incrementAndGet();
				} catch (BusinessException e) {
					if (e.getErrorCode() == ErrorCode.LOTTERY_ALREADY_DRAWN) {
						alreadyDrawn.incrementAndGet();
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
		assertThat(alreadyDrawn.get()).isEqualTo(threads - 1);
		assertThat(other.get()).isZero();
		assertThat(lotteryEntryRepository.countByEventId(eventId)).isEqualTo(candidates);
	}
}
