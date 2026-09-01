package com.studyroom.reservation.repository;

import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * 같은 룸에서 [startAt, endAt) 구간과 겹치는 활성(RESERVED) 예약이 있는지.
	 * 겹침 조건: 기존.startAt &lt; 요청.endAt AND 기존.endAt &gt; 요청.startAt
	 *
	 * <p>주의: 이 검사와 저장 사이에 락이 없어 동시 요청이 함께 통과할 수 있다.
	 * 로드맵 2단계에서 비관적 락 → Redisson 분산 락으로 해결한다.
	 */
	@Query("""
			select count(r) > 0 from Reservation r
			where r.room.id = :roomId
			  and r.status = com.studyroom.reservation.entity.ReservationStatus.RESERVED
			  and r.startAt < :endAt
			  and r.endAt > :startAt
			""")
	boolean existsOverlap(@Param("roomId") Long roomId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);

	@EntityGraph(attributePaths = {"room"})
	List<Reservation> findByMemberIdAndStatusOrderByStartAtDesc(Long memberId, ReservationStatus status);

	@EntityGraph(attributePaths = {"room"})
	List<Reservation> findByMemberIdOrderByStartAtDesc(Long memberId);

	long countByRoomIdAndStatus(Long roomId, ReservationStatus status);

	@EntityGraph(attributePaths = {"room", "member"})
	@Query("select r from Reservation r where r.id = :id")
	Optional<Reservation> findDetailById(@Param("id") Long id);
}
