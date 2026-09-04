package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/** ACTIVE PRO 구독자만 홀딩 유예가 연장된다. */
class SubscriptionBenefitTest extends SubscriptionScenarioSupport {

	@Autowired
	SubscriptionBenefit subscriptionBenefit;

	@Test
	@DisplayName("ACTIVE PRO → 20분, 구독 없음 → 10분, PAST_DUE → 10분")
	void ttl_by_subscription_state() {
		Long pro = newMember();
		subscriptionRepository.save(Subscription.subscribePro(pro, 9900));

		Long pastDue = newMember();
		Subscription lapsed = Subscription.subscribePro(pastDue, 9900);
		ReflectionTestUtils.setField(lapsed, "status", SubscriptionStatus.PAST_DUE);
		subscriptionRepository.save(lapsed);

		Long free = newMember();

		assertThat(subscriptionBenefit.ttlFor(pro)).isEqualTo(Duration.ofMinutes(20));
		assertThat(subscriptionBenefit.ttlFor(pastDue)).isEqualTo(Duration.ofMinutes(10));
		assertThat(subscriptionBenefit.ttlFor(free)).isEqualTo(Duration.ofMinutes(10));
	}
}
