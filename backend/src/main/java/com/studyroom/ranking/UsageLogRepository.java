package com.studyroom.ranking;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

	boolean existsByReservationId(Long reservationId);

	/** 회원별 누적 이용 분 (전체 랭킹 재구축용). */
	@Query("select u.memberId as memberId, sum(u.minutes) as minutes from UsageLog u group by u.memberId")
	List<MemberMinutes> sumByMember();

	/** {@code [from, to)} 구간의 회원별 이용 분 (일간 랭킹 재구축용). */
	@Query("""
			select u.memberId as memberId, sum(u.minutes) as minutes from UsageLog u
			where u.occurredAt >= :from and u.occurredAt < :to
			group by u.memberId
			""")
	List<MemberMinutes> sumByMemberBetween(@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	/** JPQL 프로젝션. */
	interface MemberMinutes {
		Long getMemberId();

		long getMinutes();
	}
}
