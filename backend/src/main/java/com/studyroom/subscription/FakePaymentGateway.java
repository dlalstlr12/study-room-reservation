package com.studyroom.subscription;

import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PG 대역. 실제 결제 대신 로그를 남기되, {@code subscription.payment.failure-rate} 확률로
 * 실패를 반환해 PAST_DUE·재결제 경로를 만든다.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

	private final SubscriptionProperties properties;

	public FakePaymentGateway(SubscriptionProperties properties) {
		this.properties = properties;
	}

	@Override
	public PaymentResult charge(String idempotencyKey, long amountKrw) {
		double failureRate = properties.payment().failureRate();
		if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
			log.warn("[결제] 실패 key={} 금액={}", idempotencyKey, amountKrw);
			return PaymentResult.failed("결제 게이트웨이 오류");
		}
		log.info("[결제] 성공 key={} 금액={}원", idempotencyKey, amountKrw);
		return PaymentResult.ok();
	}
}
