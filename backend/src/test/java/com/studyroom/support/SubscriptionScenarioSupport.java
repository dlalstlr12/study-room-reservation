package com.studyroom.support;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.subscription.OutboxEventRepository;
import com.studyroom.subscription.PaymentRepository;
import com.studyroom.subscription.Subscription;
import com.studyroom.subscription.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 구독 통합 테스트 공통 픽스처. 실제 MySQL·Redis·Kafka 컨테이너에 붙는다.
 * 컨테이너가 공유되므로 각 테스트가 만든 구독·결제·아웃박스를 끝날 때 지운다.
 */
public abstract class SubscriptionScenarioSupport extends IntegrationTest {

	@Autowired
	protected SubscriptionRepository subscriptionRepository;
	@Autowired
	protected PaymentRepository paymentRepository;
	@Autowired
	protected OutboxEventRepository outboxEventRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;

	@AfterEach
	void cleanUpSubscriptionData() {
		paymentRepository.deleteAllInBatch();
		outboxEventRepository.deleteAllInBatch();
		subscriptionRepository.deleteAllInBatch();
	}

	protected Long newMember() {
		Member member = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(member).getId();
	}

	/** 결제일이 지난 ACTIVE PRO 구독 (배치가 바로 결제 대상으로 잡는다). */
	protected Subscription dueProSubscription(Long memberId, int priceKrw) {
		Subscription subscription = Subscription.subscribePro(memberId, priceKrw);
		ReflectionTestUtils.setField(subscription, "nextBillingAt", LocalDateTime.now().minusMinutes(1));
		return subscriptionRepository.save(subscription);
	}
}
