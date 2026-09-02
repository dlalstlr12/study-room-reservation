package com.studyroom.lottery;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryEventRepository extends JpaRepository<LotteryEvent, Long> {

	List<LotteryEvent> findAllByOrderByIdDesc();
}
