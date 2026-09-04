package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** 결제 게이트웨이가 실패하면 구독은 PAST_DUE, 아웃박스는 PAYMENT_FAILED. */
@TestPropertySource(properties = "subscription.payment.failure-rate=1.0")
class PaymentFailureTest extends SubscriptionScenarioSupport {

	@Autowired
	PaymentService paymentService;

	@Test
	@DisplayName("결제 실패 → Payment FAILED + 구독 PAST_DUE + 아웃박스 PAYMENT_FAILED")
	void charge_failure() {
		Subscription sub = dueProSubscription(newMember(), 9900);

		paymentService.chargeForPeriod(sub.getId(), YearMonth.now());

		assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(subscriptionRepository.findById(sub.getId()).orElseThrow().getStatus())
				.isEqualTo(SubscriptionStatus.PAST_DUE);
		assertThat(outboxEventRepository.findAll().get(0).getEventType()).isEqualTo("PAYMENT_FAILED");
	}
}
