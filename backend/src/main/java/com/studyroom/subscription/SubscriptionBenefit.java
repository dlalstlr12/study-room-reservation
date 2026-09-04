package com.studyroom.subscription;

import com.studyroom.reservation.hold.HoldTtlPolicy;
import com.studyroom.reservation.hold.ReservationHoldProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구독 혜택을 예약 도메인에 노출한다. 현재는 홀딩 유예 연장 하나 —
 * ACTIVE PRO 구독자는 {@code subscription.benefit.pro-hold-ttl}, 그 외는 기본값.
 */
@Component
public class SubscriptionBenefit implements HoldTtlPolicy {

	private final SubscriptionRepository subscriptionRepository;
	private final Duration proHoldTtl;
	private final Duration defaultHoldTtl;

	public SubscriptionBenefit(SubscriptionRepository subscriptionRepository,
			SubscriptionProperties subscriptionProperties, ReservationHoldProperties holdProperties) {
		this.subscriptionRepository = subscriptionRepository;
		this.proHoldTtl = subscriptionProperties.benefit().proHoldTtl();
		this.defaultHoldTtl = holdProperties.ttl();
	}

	@Override
	@Transactional(readOnly = true)
	public Duration ttlFor(Long memberId) {
		return subscriptionRepository.findByMemberId(memberId)
				.filter(Subscription::isActivePro)
				.map(s -> proHoldTtl)
				.orElse(defaultHoldTtl);
	}
}
