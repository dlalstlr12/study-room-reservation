package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.YearMonth;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 배치 재실행·중복 스케줄이 겹쳐도 같은 주기는 한 번만 청구된다 (idempotency_key UNIQUE). */
class PaymentIdempotencyConcurrencyTest extends SubscriptionScenarioSupport {

	@Autowired
	PaymentService paymentService;

	@Test
	@DisplayName("8 스레드가 같은 (구독, 주기) 결제 → Payment 정확히 1건")
	void concurrent_charge_is_idempotent() throws InterruptedException {
		Subscription sub = dueProSubscription(newMember(), 9900);
		YearMonth period = YearMonth.now();
		int threads = 8;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					start.await();
					paymentService.chargeForPeriod(sub.getId(), period);
				} catch (RuntimeException | InterruptedException ignored) {
					// 경쟁에서 진 스레드는 DataIntegrityViolation 을 서비스가 삼킨다
				} finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
		pool.shutdownNow();

		assertThat(paymentRepository.countBySubscriptionId(sub.getId())).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);
	}
}
