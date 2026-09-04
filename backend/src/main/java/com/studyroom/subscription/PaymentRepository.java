package com.studyroom.subscription;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	boolean existsByIdempotencyKey(String idempotencyKey);

	List<Payment> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

	long countBySubscriptionId(Long subscriptionId);
}
