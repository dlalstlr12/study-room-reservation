package com.studyroom.notification;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

	List<Notification> findByMemberIdAndReadAtIsNullOrderByIdDesc(Long memberId, Pageable pageable);

	long countByMemberIdAndReadAtIsNull(Long memberId);

	/** 멱등 가드 — 워커가 저장 전에 확인한다. */
	boolean existsByDedupKey(String dedupKey);

	java.util.Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

	@Modifying
	@Query("update Notification n set n.readAt = CURRENT_TIMESTAMP "
			+ "where n.memberId = :memberId and n.readAt is null")
	int markAllRead(@Param("memberId") Long memberId);
}
