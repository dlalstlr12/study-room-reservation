package com.studyroom.reservation;

import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.reservation.service.ReservationService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동 퇴실 백스톱. 사용자가 "퇴실하기"를 누르지 않아도 예약 종료 시각이 지나면
 * 이용 완료로 전환해 랭킹 집계로 흘려보낸다. 3단계 홀딩 스윕과 같은 패턴 —
 * 수동 퇴실이 즉시성을, 이 스케줄러가 최종 정합성을 담당한다.
 */
@Component
public class ReservationCheckoutScheduler {

	private static final Logger log = LoggerFactory.getLogger(ReservationCheckoutScheduler.class);

	private final ReservationRepository reservationRepository;
	private final ReservationService reservationService;

	public ReservationCheckoutScheduler(ReservationRepository reservationRepository,
			ReservationService reservationService) {
		this.reservationRepository = reservationRepository;
		this.reservationService = reservationService;
	}

	@Scheduled(fixedDelayString = "${reservation.checkout.sweep-ms:60000}",
			initialDelayString = "${reservation.checkout.sweep-ms:60000}")
	public void sweep() {
		List<Reservation> due = reservationRepository.findByStatusAndEndAtLessThanEqual(
				ReservationStatus.RESERVED, LocalDateTime.now());
		if (due.isEmpty()) {
			return;
		}
		log.debug("자동 퇴실 대상 {}건", due.size());
		for (Reservation reservation : due) {
			try {
				reservationService.autoComplete(reservation.getId());
			} catch (RuntimeException e) {
				log.warn("자동 퇴실 실패: reservation={}", reservation.getId(), e);
			}
		}
	}
}
