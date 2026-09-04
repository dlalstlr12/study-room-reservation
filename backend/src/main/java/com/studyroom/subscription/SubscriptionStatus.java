package com.studyroom.subscription;

/** 구독 상태. */
public enum SubscriptionStatus {

	/** 정상 — 다음 결제일까지 혜택 유효 */
	ACTIVE,
	/** 결제 실패 — 혜택 정지, 재결제 대기 */
	PAST_DUE,
	/** 해지됨 */
	CANCELLED
}
