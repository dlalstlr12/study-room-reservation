package com.studyroom.lottery;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryEventRepository extends JpaRepository<LotteryEvent, Long> {

	/** 추첨 시각이 지난 대기 이벤트 (스케줄러용). */
	List<LotteryEvent> findByStatusAndDrawAtLessThanEqual(LotteryEventStatus status, LocalDateTime at);

	List<LotteryEvent> findAllByOrderByDrawAtDesc();
}
