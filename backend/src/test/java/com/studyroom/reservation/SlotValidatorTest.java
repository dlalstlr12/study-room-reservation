package com.studyroom.reservation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlotValidatorTest {

	private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 5, 10, 0);

	@Test
	void 정시_시작_30분_배수_길이는_통과() {
		assertThatCode(() -> SlotValidator.validate(BASE, BASE.plusMinutes(30))).doesNotThrowAnyException();
		assertThatCode(() -> SlotValidator.validate(BASE.plusMinutes(30), BASE.plusHours(4).plusMinutes(30)))
				.doesNotThrowAnyException();
	}

	@Test
	void 분이_0또는30이_아니면_거부() {
		assertThatThrownBy(() -> SlotValidator.validate(BASE.plusMinutes(15), BASE.plusMinutes(45)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_RESERVATION_TIME);
	}

	@Test
	void 초나_나노가_0이_아니면_거부() {
		assertThatThrownBy(() -> SlotValidator.validate(BASE.withSecond(1), BASE.plusHours(1)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void 길이가_30분_배수가_아니면_거부() {
		// 시작·끝은 정렬됐지만 길이 계산이 30분 배수가 아닌 경우는 정렬 규칙상 나올 수 없으므로
		// 최소 길이 위반으로 검증한다.
		assertThatThrownBy(() -> SlotValidator.validate(BASE, BASE))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_RESERVATION_TIME);
	}

	@Test
	void 최대_4시간_초과는_거부() {
		assertThatThrownBy(() -> SlotValidator.validate(BASE, BASE.plusHours(4).plusMinutes(30)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_RESERVATION_TIME);
	}

	@Test
	void 시작이_종료_이후면_거부() {
		assertThatThrownBy(() -> SlotValidator.validate(BASE.plusHours(1), BASE))
				.isInstanceOf(BusinessException.class);
	}
}
