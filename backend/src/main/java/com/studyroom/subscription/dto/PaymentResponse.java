package com.studyroom.subscription.dto;

import com.studyroom.subscription.Payment;
import com.studyroom.subscription.PaymentStatus;
import java.time.LocalDateTime;

/** 결제 이력 한 건. */
public record PaymentResponse(
		Long id,
		int amountKrw,
		PaymentStatus status,
		String failureReason,
		LocalDateTime paidAt
) {
	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getAmountKrw(),
				payment.getStatus(),
				payment.getFailureReason(),
				payment.getCreatedAt());
	}
}
