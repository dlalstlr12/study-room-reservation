package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.service.ReservationService;
import com.studyroom.support.RankingScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 수동 퇴실 → COMPLETED 전이 + 이벤트 → 랭킹 반영. */
class ReservationCheckoutTest extends RankingScenarioSupport {

	@Autowired
	ReservationService reservationService;

	@Test
	@DisplayName("checkout → COMPLETED + checkedOutAt + 랭킹 집계")
	void checkout_completes_and_ranks() {
		Long memberId = newMember();
		LocalDateTime now = LocalDateTime.now();
		Reservation reservation = reservation(memberId, newRoom(), now.minusMinutes(90), now.plusMinutes(30));

		reservationService.checkout(reservation.getId(), memberId);

		Reservation reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(reloaded.getCheckedOutAt()).isNotNull();

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
				assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId))
						.isGreaterThanOrEqualTo(89).isLessThanOrEqualTo(91)); // ~90분
	}

	@Test
	@DisplayName("이미 완료된 예약은 다시 퇴실할 수 없다 (409)")
	void cannot_checkout_twice() {
		Long memberId = newMember();
		LocalDateTime now = LocalDateTime.now();
		Reservation reservation = reservation(memberId, newRoom(), now.minusMinutes(30), now.plusMinutes(30));

		reservationService.checkout(reservation.getId(), memberId);

		assertThatThrownBy(() -> reservationService.checkout(reservation.getId(), memberId))
				.isInstanceOfSatisfying(BusinessException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_COMPLETABLE));
	}

	@Test
	@DisplayName("남의 예약은 퇴실할 수 없다 (403)")
	void cannot_checkout_others() {
		Long owner = newMember();
		Long other = newMember();
		LocalDateTime now = LocalDateTime.now();
		Reservation reservation = reservation(owner, newRoom(), now.minusMinutes(30), now.plusMinutes(30));

		assertThatThrownBy(() -> reservationService.checkout(reservation.getId(), other))
				.isInstanceOfSatisfying(BusinessException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED));
	}
}
