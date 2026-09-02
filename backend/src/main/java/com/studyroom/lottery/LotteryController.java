package com.studyroom.lottery;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.lottery.dto.LotteryEntryResponse;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.lottery.dto.LotteryEventResponse;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lottery", description = "이벤트 추첨 — 이용 중이던 회원 대상 (인증 필요, 생성·추첨은 ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/lottery/events")
public class LotteryController {

	private final LotteryService lotteryService;

	public LotteryController(LotteryService lotteryService) {
		this.lotteryService = lotteryService;
	}

	@Operation(summary = "추첨 이벤트 생성", description = "ADMIN. drawAt 도래 시 스케줄러가 자동 추첨한다.")
	@PostMapping
	public ResponseEntity<LotteryEventResponse> create(
			@Valid @RequestBody LotteryEventCreateRequest request) {
		LotteryEventResponse created = lotteryService.createEvent(request);
		return ResponseEntity.created(URI.create("/api/lottery/events/" + created.id())).body(created);
	}

	@Operation(summary = "추첨 이벤트 목록")
	@GetMapping
	public List<LotteryEventResponse> list(@AuthenticationPrincipal MemberPrincipal principal) {
		return lotteryService.getEvents(principal.memberId());
	}

	@Operation(summary = "내 참여 이력")
	@GetMapping("/me")
	public List<LotteryEntryResponse> myEntries(@AuthenticationPrincipal MemberPrincipal principal) {
		return lotteryService.myEntries(principal.memberId());
	}

	@Operation(summary = "추첨 이벤트 상세", description = "당첨자 목록과 내 결과.")
	@GetMapping("/{eventId}")
	public LotteryEventResponse get(@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long eventId) {
		return lotteryService.getEvent(eventId, principal.memberId());
	}

	@Operation(summary = "수동 추첨", description = "ADMIN. drawAt 전이라도 즉시 추첨. 이미 추첨됐으면 409.")
	@PostMapping("/{eventId}/draw")
	public LotteryEventResponse draw(@PathVariable Long eventId) {
		return lotteryService.draw(eventId, true);
	}
}
