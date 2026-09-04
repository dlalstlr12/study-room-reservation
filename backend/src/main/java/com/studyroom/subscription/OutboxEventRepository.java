package com.studyroom.subscription;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	/**
	 * 미발행 이벤트를 잠그고 가져온다. {@code SKIP LOCKED} 라 릴레이 인스턴스가 여러 개여도
	 * 같은 행을 두 번 발행하지 않는다. 호출부는 트랜잭션 안에서 써야 한다.
	 */
	@Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL "
			+ "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
	List<OutboxEvent> lockUnpublished(@Param("limit") int limit);

	long countByPublishedAtIsNull();
}
