package com.studyroom.subscription;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 구독 설정. {@code subscription.*}
 *
 * @param plan    플랜 가격
 * @param benefit 예약 도메인과 연계되는 혜택
 * @param payment 결제 게이트웨이 동작
 * @param outbox  아웃박스 릴레이 폴 주기
 */
@ConfigurationProperties("subscription")
public record SubscriptionProperties(Plan plan, Benefit benefit, PaymentGatewayConfig payment,
		Outbox outbox) {

	public SubscriptionProperties {
		if (plan == null) {
			plan = new Plan(9900);
		}
		if (benefit == null) {
			benefit = new Benefit(Duration.ofMinutes(20));
		}
		if (payment == null) {
			payment = new PaymentGatewayConfig(0.0);
		}
		if (outbox == null) {
			outbox = new Outbox(2000L);
		}
	}

	public record Plan(int proPriceKrw) {
	}

	/** @param proHoldTtl PRO 구독자 홀딩 유예 시간 */
	public record Benefit(Duration proHoldTtl) {
		public Benefit {
			if (proHoldTtl == null || proHoldTtl.isZero() || proHoldTtl.isNegative()) {
				proHoldTtl = Duration.ofMinutes(20);
			}
		}
	}

	/** @param failureRate 결제 실패 시뮬레이션 확률 (0.0~1.0) */
	public record PaymentGatewayConfig(double failureRate) {
		public PaymentGatewayConfig {
			failureRate = Math.min(Math.max(failureRate, 0.0), 1.0);
		}
	}

	public record Outbox(long pollMs) {
		public Outbox {
			if (pollMs <= 0) {
				pollMs = 2000L;
			}
		}
	}
}
