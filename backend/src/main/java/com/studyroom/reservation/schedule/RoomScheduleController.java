package com.studyroom.reservation.schedule;

import com.studyroom.common.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "스터디룸 조회(공개) 및 관리(ADMIN)")
@RestController
@RequestMapping("/api/rooms/{roomId}/schedule")
public class RoomScheduleController {

	private final RoomScheduleService roomScheduleService;

	public RoomScheduleController(RoomScheduleService roomScheduleService) {
		this.roomScheduleService = roomScheduleService;
	}

	@Operation(summary = "룸 예약 현황", description = "해당 날짜의 예약·홀딩 구간. 인증 불필요(로그인 시 내 것 표시).")
	@GetMapping
	public RoomScheduleResponse getSchedule(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long roomId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		LocalDate target = (date != null) ? date : LocalDate.now();
		Long viewerMemberId = (principal != null) ? principal.memberId() : null;
		return roomScheduleService.getSchedule(roomId, target, viewerMemberId);
	}
}
