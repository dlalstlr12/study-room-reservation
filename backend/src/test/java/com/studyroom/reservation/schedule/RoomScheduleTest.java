package com.studyroom.reservation.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.reservation.hold.HoldResponse;
import com.studyroom.support.HoldScenarioSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoomScheduleTest extends HoldScenarioSupport {

	@Autowired
	private RoomScheduleService roomScheduleService;

	@Test
	@DisplayName("현황판은 확정 예약과 활성 홀딩을 시작 시각 순으로 합친다")
	void schedule_merges_reservations_and_holds() {
		long roomId = newRoom().getId();
		Long me = newMember();
		Long other = newMember();

		// 14~15 확정 예약
		HoldResponse toConfirm = holdService.hold(other, holdRequest(roomId, 14, 1));
		holdService.confirm(other, roomId, toConfirm.holdId());
		// 10~11 내 홀딩
		holdService.hold(me, holdRequest(roomId, 10, 1));

		LocalDate date = tomorrowAt(0).toLocalDate();
		RoomScheduleResponse schedule = roomScheduleService.getSchedule(roomId, date, me);

		assertThat(schedule.entries()).hasSize(2);
		assertThat(schedule.entries().get(0).type()).isEqualTo(ScheduleEntryType.HOLDING);
		assertThat(schedule.entries().get(0).mine()).isTrue();
		assertThat(schedule.entries().get(1).type()).isEqualTo(ScheduleEntryType.RESERVED);
		assertThat(schedule.entries().get(1).mine()).isFalse();
	}

	@Test
	@DisplayName("다른 날짜는 비어 있다")
	void other_date_is_empty() {
		long roomId = newRoom().getId();
		holdService.hold(newMember(), holdRequest(roomId, 10, 1));

		RoomScheduleResponse schedule = roomScheduleService.getSchedule(
				roomId, tomorrowAt(0).toLocalDate().plusDays(3), null);

		assertThat(schedule.entries()).isEmpty();
	}
}
