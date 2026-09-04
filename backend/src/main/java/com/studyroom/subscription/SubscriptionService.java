package com.studyroom.subscription;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.subscription.dto.PaymentResponse;
import com.studyroom.subscription.dto.SubscriptionResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 구독 가입·해지·조회. 결제는 {@link PaymentService}(배치)가 담당한다. */
@Service
public class SubscriptionService {

	private static final int PAYMENT_PAGE = 20;

	private final SubscriptionRepository subscriptionRepository;
	private final PaymentRepository paymentRepository;
	private final SubscriptionProperties properties;

	public SubscriptionService(SubscriptionRepository subscriptionRepository,
			PaymentRepository paymentRepository, SubscriptionProperties properties) {
		this.subscriptionRepository = subscriptionRepository;
		this.paymentRepository = paymentRepository;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public SubscriptionResponse mySubscription(Long memberId) {
		return subscriptionRepository.findByMemberId(memberId)
				.map(SubscriptionResponse::from)
				.orElseGet(SubscriptionResponse::free);
	}

	/** PRO 구독 시작(또는 해지·연체 상태에서 재개). 다음 배치가 첫 결제를 잡는다. */
	@Transactional
	public SubscriptionResponse subscribePro(Long memberId) {
		int price = properties.plan().proPriceKrw();
		Subscription subscription = subscriptionRepository.findByMemberId(memberId).orElse(null);
		if (subscription == null) {
			subscription = subscriptionRepository.save(Subscription.subscribePro(memberId, price));
		} else if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
		} else {
			subscription.resumePro(price);
		}
		return SubscriptionResponse.from(subscription);
	}

	@Transactional
	public SubscriptionResponse cancel(Long memberId) {
		Subscription subscription = subscriptionRepository.findByMemberId(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
		subscription.cancel();
		return SubscriptionResponse.from(subscription);
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> myPayments(Long memberId) {
		return paymentRepository.findByMemberIdOrderByIdDesc(memberId, PageRequest.of(0, PAYMENT_PAGE))
				.stream().map(PaymentResponse::from).toList();
	}
}
