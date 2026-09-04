package com.studyroom.subscription;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 시도 한 건. {@code idempotency_key}(`sub:{id}:{yyyy-MM}`) UNIQUE 로 배치 재실행·중복
 * 스케줄에도 같은 주기가 두 번 청구되지 않는다.
 */
@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subscription_id", nullable = false)
	private Long subscriptionId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "amount_krw", nullable = false)
	private int amountKrw;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "failure_reason", length = 200)
	private String failureReason;

	private Payment(Subscription subscription, String idempotencyKey, PaymentStatus status,
			String failureReason) {
		this.subscriptionId = subscription.getId();
		this.memberId = subscription.getMemberId();
		this.amountKrw = subscription.getPriceKrw();
		this.idempotencyKey = idempotencyKey;
		this.status = status;
		this.failureReason = failureReason;
	}

	public static Payment succeeded(Subscription subscription, String idempotencyKey) {
		return new Payment(subscription, idempotencyKey, PaymentStatus.SUCCEEDED, null);
	}

	public static Payment failed(Subscription subscription, String idempotencyKey, String reason) {
		return new Payment(subscription, idempotencyKey, PaymentStatus.FAILED, reason);
	}

	public boolean isSucceeded() {
		return status == PaymentStatus.SUCCEEDED;
	}
}
