package com.studyroom.reservation;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 예약·홀딩 공통 시간 규칙. 예약은 <b>30분 단위</b>로만 가능하다.
 *
 * <ul>
 *   <li>시작·종료 시각의 분은 0 또는 30, 초·나노초는 0</li>
 *   <li>길이는 30분의 배수, 최소 30분 · 최대 4시간</li>
 * </ul>
 *
 * 슬롯을 고정하면 프론트에서 타임라인을 눈금으로 그릴 수 있고, 겹침 검사도 단순해진다.
 */
public final class SlotValidator {

	static final int SLOT_MINUTES = 30;
	static final Duration MIN_DURATION = Duration.ofMinutes(SLOT_MINUTES);
	static final Duration MAX_DURATION = Duration.ofHours(4);

	private SlotValidator() {
	}

	public static void validate(LocalDateTime startAt, LocalDateTime endAt) {
		if (!isAligned(startAt) || !isAligned(endAt)) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME,
					"예약은 30분 단위(정시 또는 30분)로만 가능합니다.");
		}
		if (!startAt.isBefore(endAt)) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME,
					"시작 시각은 종료 시각보다 앞서야 합니다.");
		}
		Duration length = Duration.between(startAt, endAt);
		if (length.toMinutes() % SLOT_MINUTES != 0) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME,
					"예약 길이는 30분 단위여야 합니다.");
		}
		if (length.compareTo(MIN_DURATION) < 0 || length.compareTo(MAX_DURATION) > 0) {
			throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME,
					"예약 길이는 " + MIN_DURATION.toMinutes() + "분 이상 "
							+ MAX_DURATION.toHours() + "시간 이하여야 합니다.");
		}
	}

	private static boolean isAligned(LocalDateTime time) {
		return (time.getMinute() == 0 || time.getMinute() == SLOT_MINUTES)
				&& time.getSecond() == 0 && time.getNano() == 0;
	}
}
