package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LotteryEventTest {

	private LotteryEvent scheduled() {
		return LotteryEvent.create("상품 추첨", "기프티콘", LotteryAudience.ALL_USERS, 1);
	}

	@Test
	@DisplayName("당첨 인원이 1 미만이면 생성 불가")
	void winner_count_must_be_positive() {
		assertThatThrownBy(() -> LotteryEvent.create("t", "p", LotteryAudience.ALL_USERS, 0))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOTTERY_INVALID_SCHEDULE);
	}

	@Test
	@DisplayName("대상이 없으면 생성 불가")
	void audience_required() {
		assertThatThrownBy(() -> LotteryEvent.create("t", "p", null, 1))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOTTERY_INVALID_SCHEDULE);
	}

	@Test
	@DisplayName("markDrawn은 한 번만 — 두 번째는 LOTTERY_ALREADY_DRAWN")
	void mark_drawn_is_idempotent_guard() {
		LotteryEvent event = scheduled();
		assertThatCode(() -> event.markDrawn(42L)).doesNotThrowAnyException();
		assertThat(event.getStatus()).isEqualTo(LotteryEventStatus.DRAWN);
		assertThat(event.getSeed()).isEqualTo(42L);

		assertThatThrownBy(() -> event.markDrawn(99L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOTTERY_ALREADY_DRAWN);
	}
}
