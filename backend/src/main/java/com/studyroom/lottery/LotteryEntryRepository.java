package com.studyroom.lottery;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryEntryRepository extends JpaRepository<LotteryEntry, Long> {

	List<LotteryEntry> findByEventId(Long eventId);

	List<LotteryEntry> findByEventIdIn(java.util.Collection<Long> eventIds);

	List<LotteryEntry> findByEventIdAndWinnerTrue(Long eventId);

	List<LotteryEntry> findByMemberIdOrderByEventIdDesc(Long memberId);

	boolean existsByEventId(Long eventId);

	long countByEventId(Long eventId);

	long countByEventIdAndWinnerTrue(Long eventId);

	void deleteByEventId(Long eventId);
}
