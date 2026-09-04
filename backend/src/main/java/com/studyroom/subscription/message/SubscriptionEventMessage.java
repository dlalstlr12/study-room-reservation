package com.studyroom.subscription.message;

import java.time.LocalDateTime;

/**
 * 아웃박스 payload이자 Kafka {@code subscription-events} 메시지. 결제 결과 한 건.
 *
 * @param eventType {@code PAYMENT_SUCCEEDED} | {@code PAYMENT_FAILED}
 */
public record SubscriptionEventMessage(
		String eventType,
		Long subscriptionId,
		Long memberId,
		String plan,
		int amountKrw,
		LocalDateTime occurredAt,
		String failureReason
) {
	public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
	public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
}
