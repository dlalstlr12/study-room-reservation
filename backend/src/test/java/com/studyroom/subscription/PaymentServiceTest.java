package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 정기 결제 — 성공/실패 처리, 멱등, 아웃박스 적재. */
class PaymentServiceTest extends SubscriptionScenarioSupport {

	@Autowired
	PaymentService paymentService;

	private final YearMonth period = YearMonth.now();

	@Test
	@DisplayName("결제 성공 → Payment SUCCEEDED + 구독 renew + 아웃박스 PAYMENT_SUCCEEDED")
	void charge_success() {
		Subscription sub = dueProSubscription(newMember(), 9900);

		paymentService.chargeForPeriod(sub.getId(), period);

		List<Payment> payments = paymentRepository.findAll();
		assertThat(payments).hasSize(1);
		assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

		Subscription reloaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(reloaded.getNextBillingAt()).isAfter(java.time.LocalDateTime.now());

		List<OutboxEvent> outbox = outboxEventRepository.findAll();
		assertThat(outbox).hasSize(1);
		assertThat(outbox.get(0).getEventType()).isEqualTo("PAYMENT_SUCCEEDED");
		assertThat(outbox.get(0).getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("같은 (구독, 주기) 두 번 결제 → Payment는 한 건 (멱등)")
	void idempotent_per_period() {
		Subscription sub = dueProSubscription(newMember(), 9900);

		paymentService.chargeForPeriod(sub.getId(), period);
		paymentService.chargeForPeriod(sub.getId(), period);

		assertThat(paymentRepository.countBySubscriptionId(sub.getId())).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);
	}
}
