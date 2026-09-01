package com.studyroom.reservation.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.reservation.dto.ReservationResponse;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.support.HoldScenarioSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HoldLifecycleTest extends HoldScenarioSupport {

	@Test
	@DisplayName("홀딩 → 확정 시 RESERVED 예약이 생기고 홀딩은 사라진다")
	void hold_then_confirm() {
		long roomId = newRoom().getId();
		Long memberId = newMember();

		HoldResponse hold = holdService.hold(memberId, holdRequest(roomId, 10, 1));
		assertThat(holdRepository.find(roomId, hold.holdId())).isPresent();

		ReservationResponse reservation = holdService.confirm(memberId, roomId, hold.holdId());

		assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(holdRepository.find(roomId, hold.holdId())).isEmpty();
		assertThat(reservationRepository.countByRoomIdAndStatus(roomId, ReservationStatus.RESERVED))
				.isEqualTo(1);
	}

	@Test
	@DisplayName("겹치는 시간대는 홀딩할 수 없다 (RESERVATION_HOLD_CONFLICT)")
	void overlapping_hold_rejected() {
		long roomId = newRoom().getId();
		holdService.hold(newMember(), holdRequest(roomId, 10, 2)); // 10~12

		assertThatThrownBy(() -> holdService.hold(newMember(), holdRequest(roomId, 11, 2))) // 11~13
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_HOLD_CONFLICT);
	}

	@Test
	@DisplayName("해제한 홀딩은 확정할 수 없다")
	void released_hold_cannot_confirm() {
		long roomId = newRoom().getId();
		Long memberId = newMember();
		HoldResponse hold = holdService.hold(memberId, holdRequest(roomId, 14, 1));

		holdService.release(memberId, roomId, hold.holdId());

		assertThatThrownBy(() -> holdService.confirm(memberId, roomId, hold.holdId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_HOLD_NOT_FOUND);
	}

	@Test
	@DisplayName("남의 홀딩은 확정·해제할 수 없다 (RESERVATION_ACCESS_DENIED)")
	void cannot_touch_others_hold() {
		long roomId = newRoom().getId();
		Long owner = newMember();
		Long stranger = newMember();
		HoldResponse hold = holdService.hold(owner, holdRequest(roomId, 16, 1));

		assertThatThrownBy(() -> holdService.confirm(stranger, roomId, hold.holdId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED);
		assertThatThrownBy(() -> holdService.release(stranger, roomId, hold.holdId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED);
	}

	@Test
	@DisplayName("내 홀딩 목록 조회")
	void my_holds() {
		Long memberId = newMember();
		long roomA = newRoom().getId();
		long roomB = newRoom().getId();
		holdService.hold(memberId, holdRequest(roomA, 10, 1));
		holdService.hold(memberId, holdRequest(roomB, 12, 1));

		assertThat(holdService.myHolds(memberId)).hasSize(2);
	}
}
