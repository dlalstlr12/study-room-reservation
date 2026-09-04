package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.reservation.ReservationCheckoutScheduler;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.support.RankingScenarioSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** endAt 이 지난 RESERVED 예약은 백스톱 스케줄러가 자동 완료한다. */
class ReservationCheckoutSchedulerTest extends RankingScenarioSupport {

	@Autowired
	ReservationCheckoutScheduler scheduler;

	@Test
	@DisplayName("sweep → 종료 시각 지난 예약이 COMPLETED, 이용시간 = 예약 전구간")
	void auto_completes_past_reservations() {
		Long memberId = newMember();
		LocalDateTime now = LocalDateTime.now();
		Reservation reservation = reservation(memberId, newRoom(),
				now.minusMinutes(50), now.minusMinutes(20)); // 30분, 이미 종료

		scheduler.sweep();

		Reservation reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(reloaded.getCheckedOutAt()).isCloseTo(reservation.getEndAt(),
				org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
		assertThat(reloaded.usedMinutes()).isEqualTo(30);
	}
}
