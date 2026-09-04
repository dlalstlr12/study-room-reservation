package com.studyroom.subscription;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 구독 건의 정기 결제.
 *
 * <ul>
 *   <li><b>멱등</b>: {@code idempotency_key}(`sub:{id}:{yyyy-MM}`) UNIQUE — 배치 재실행·중복
 *       스케줄에도 같은 주기가 두 번 청구되지 않는다.</li>
 *   <li><b>아웃박스</b>: 결제(payment) · 상태 변경(subscription) · 이벤트(outbox_event) 저장이
 *       한 트랜잭션. 발행은 릴레이가 하므로 브로커 장애와 무관하다.</li>
 *   <li><b>격리</b>: {@code REQUIRES_NEW} — 배치 청크 트랜잭션과 분리해 한 건 실패가 다른 건을
 *       롤백하지 않는다.</li>
 * </ul>
 */
@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final SubscriptionRepository subscriptionRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentGateway paymentGateway;
	private final OutboxAppender outbox;

	public PaymentService(SubscriptionRepository subscriptionRepository,
			PaymentRepository paymentRepository, PaymentGateway paymentGateway, OutboxAppender outbox) {
		this.subscriptionRepository = subscriptionRepository;
		this.paymentRepository = paymentRepository;
		this.paymentGateway = paymentGateway;
		this.outbox = outbox;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void chargeForPeriod(Long subscriptionId, YearMonth period) {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

		String idempotencyKey = "sub:" + subscriptionId + ":" + period;
		if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
			log.debug("[결제] 이번 주기 이미 처리 key={}", idempotencyKey);
			return;
		}

		PaymentResult result = paymentGateway.charge(idempotencyKey, subscription.getPriceKrw());
		try {
			if (result.succeeded()) {
				paymentRepository.save(Payment.succeeded(subscription, idempotencyKey));
				subscription.renew();
				outbox.append(event(subscription, SubscriptionEventMessage.PAYMENT_SUCCEEDED, null));
			} else {
				paymentRepository.save(Payment.failed(subscription, idempotencyKey, result.reason()));
				subscription.markPastDue();
				outbox.append(event(subscription, SubscriptionEventMessage.PAYMENT_FAILED, result.reason()));
			}
		} catch (DataIntegrityViolationException duplicate) {
			// idempotency_key UNIQUE — 동시 배치 실행이 겹쳐도 결제는 한 번만.
			log.debug("[결제] 저장 경합, 중복으로 처리 key={}", idempotencyKey);
		}
	}

	private SubscriptionEventMessage event(Subscription subscription, String eventType, String reason) {
		return new SubscriptionEventMessage(
				eventType, subscription.getId(), subscription.getMemberId(),
				subscription.getPlan().name(), subscription.getPriceKrw(), LocalDateTime.now(), reason);
	}
}
