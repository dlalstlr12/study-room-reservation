package com.studyroom.reservation.controller;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.member.entity.MemberRole;
import com.studyroom.reservation.dto.ReservationCreateRequest;
import com.studyroom.reservation.dto.ReservationResponse;
import com.studyroom.reservation.entity.ReservationStatus;
import com.studyroom.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservation", description = "예약 생성/조회/취소 (인증 필요)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@Operation(summary = "예약 생성", description = "동시성 제어 없음(로드맵 2단계에서 락 도입 예정).")
	@PostMapping
	public ResponseEntity<ReservationResponse> create(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Valid @RequestBody ReservationCreateRequest request) {
		ReservationResponse created = reservationService.create(principal.memberId(), request);
		return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
	}

	@Operation(summary = "내 예약 목록", description = "status 로 상태 필터링 가능.")
	@GetMapping("/me")
	public List<ReservationResponse> getMyReservations(
			@AuthenticationPrincipal MemberPrincipal principal,
			@RequestParam(required = false) ReservationStatus status) {
		return reservationService.getMyReservations(principal.memberId(), status);
	}

	@Operation(summary = "예약 상세", description = "본인 예약 또는 ADMIN만 조회 가능.")
	@GetMapping("/{reservationId}")
	public ReservationResponse getReservation(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long reservationId) {
		boolean isAdmin = principal.role() == MemberRole.ADMIN;
		return reservationService.getReservation(reservationId, principal.memberId(), isAdmin);
	}

	@Operation(summary = "예약 취소", description = "본인의 RESERVED 상태 예약만 취소 가능.")
	@PostMapping("/{reservationId}/cancel")
	public ReservationResponse cancel(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long reservationId) {
		return reservationService.cancel(reservationId, principal.memberId());
	}
}
