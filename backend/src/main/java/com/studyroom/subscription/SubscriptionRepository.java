package com.studyroom.subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByMemberId(Long memberId);

	/** 결제일이 도래한 ACTIVE 구독 (배치 리더용, 페이지). */
	List<Subscription> findByStatusAndNextBillingAtLessThanEqualOrderById(
			SubscriptionStatus status, LocalDateTime at, Pageable pageable);

	long countByStatus(SubscriptionStatus status);
}
