package com.studyroom.reservation.hold;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.reservation.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hold", description = "좌석 홀딩 — 룸 선택 후 확정까지 TTL만큼 자리를 잡아둔다 (인증 필요)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reservations/holds")
public class HoldController {

	private final HoldService holdService;

	public HoldController(HoldService holdService) {
		this.holdService = holdService;
	}

	@Operation(summary = "홀딩 생성", description = "겹치는 예약·홀딩이 있으면 409. 기본 10분 뒤 자동 만료.")
	@PostMapping
	public ResponseEntity<HoldResponse> hold(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Valid @RequestBody HoldCreateRequest request) {
		HoldResponse held = holdService.hold(principal.memberId(), request);
		return ResponseEntity
				.created(URI.create("/api/reservations/holds/" + held.roomId() + "/" + held.holdId()))
				.body(held);
	}

	@Operation(summary = "내 홀딩 목록", description = "아직 확정하지 않은 내 홀딩.")
	@GetMapping("/me")
	public List<HoldResponse> myHolds(@AuthenticationPrincipal MemberPrincipal principal) {
		return holdService.myHolds(principal.memberId());
	}

	@Operation(summary = "홀딩 확정", description = "홀딩을 실제 예약(RESERVED)으로 전환한다. 만료됐으면 404.")
	@PostMapping("/{roomId}/{holdId}/confirm")
	public ResponseEntity<ReservationResponse> confirm(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long roomId,
			@PathVariable String holdId) {
		ReservationResponse created = holdService.confirm(principal.memberId(), roomId, holdId);
		return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
	}

	@Operation(summary = "홀딩 해제", description = "확정하지 않고 자리를 즉시 반납한다.")
	@DeleteMapping("/{roomId}/{holdId}")
	public ResponseEntity<Void> release(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long roomId,
			@PathVariable String holdId) {
		holdService.release(principal.memberId(), roomId, holdId);
		return ResponseEntity.noContent().build();
	}
}
