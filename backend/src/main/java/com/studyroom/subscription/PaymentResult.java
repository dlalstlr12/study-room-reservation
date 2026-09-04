package com.studyroom.subscription;

/** 결제 게이트웨이 응답. */
public record PaymentResult(boolean succeeded, String reason) {

	public static PaymentResult ok() {
		return new PaymentResult(true, null);
	}

	public static PaymentResult failed(String reason) {
		return new PaymentResult(false, reason);
	}
}
