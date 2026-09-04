package com.studyroom.subscription;

import com.studyroom.common.entity.BaseTimeEntity;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 정기 구독. 회원당 최대 하나(`member_id` UNIQUE).
 *
 * <p>{@code nextBillingAt} 이 도래하면 배치가 결제를 시도한다. 성공하면 {@link #renew()} 로
 * 다음 달로 넘기고, 실패하면 {@link #markPastDue()} 로 혜택을 정지한다.
 */
@Entity
@Getter
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionPlan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionStatus status;

	@Column(name = "price_krw", nullable = false)
	private int priceKrw;

	private LocalDateTime nextBillingAt;

	@Column(nullable = false)
	private LocalDateTime startedAt;

	private Subscription(Long memberId, SubscriptionPlan plan, int priceKrw) {
		this.memberId = memberId;
		this.plan = plan;
		this.priceKrw = priceKrw;
		this.status = SubscriptionStatus.ACTIVE;
		this.startedAt = LocalDateTime.now();
		this.nextBillingAt = LocalDateTime.now(); // 다음 배치가 첫 결제를 잡는다
	}

	public static Subscription subscribePro(Long memberId, int priceKrw) {
		return new Subscription(memberId, SubscriptionPlan.PRO, priceKrw);
	}

	/** 결제 성공 — 다음 결제일을 한 달 뒤로. PAST_DUE 였다면 복구된다. */
	public void renew() {
		LocalDateTime base = (nextBillingAt != null) ? nextBillingAt : LocalDateTime.now();
		this.nextBillingAt = base.plusMonths(1);
		this.status = SubscriptionStatus.ACTIVE;
	}

	/** 결제 실패 — 혜택 정지. 다음 배치가 다시 시도한다. */
	public void markPastDue() {
		this.status = SubscriptionStatus.PAST_DUE;
	}

	public void cancel() {
		if (status == SubscriptionStatus.CANCELLED) {
			throw new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "이미 해지된 구독입니다.");
		}
		this.status = SubscriptionStatus.CANCELLED;
		this.nextBillingAt = null;
	}

	/** PRO 재구독 (해지했던 회원). */
	public void resumePro(int priceKrw) {
		this.plan = SubscriptionPlan.PRO;
		this.priceKrw = priceKrw;
		this.status = SubscriptionStatus.ACTIVE;
		this.startedAt = LocalDateTime.now();
		this.nextBillingAt = LocalDateTime.now();
	}

	public boolean isActivePro() {
		return status == SubscriptionStatus.ACTIVE && plan == SubscriptionPlan.PRO;
	}
}
