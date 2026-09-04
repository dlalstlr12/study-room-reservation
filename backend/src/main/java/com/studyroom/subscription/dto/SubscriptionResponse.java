package com.studyroom.subscription.dto;

import com.studyroom.subscription.Subscription;
import com.studyroom.subscription.SubscriptionPlan;
import com.studyroom.subscription.SubscriptionStatus;
import java.time.LocalDateTime;

/** 내 구독 상태. 구독한 적 없으면 {@code plan=FREE, status=null}. */
public record SubscriptionResponse(
		SubscriptionPlan plan,
		SubscriptionStatus status,
		int priceKrw,
		LocalDateTime nextBillingAt,
		LocalDateTime startedAt
) {
	public static SubscriptionResponse free() {
		return new SubscriptionResponse(SubscriptionPlan.FREE, null, 0, null, null);
	}

	public static SubscriptionResponse from(Subscription subscription) {
		return new SubscriptionResponse(
				subscription.getPlan(),
				subscription.getStatus(),
				subscription.getPriceKrw(),
				subscription.getNextBillingAt(),
				subscription.getStartedAt());
	}
}
