package com.studyroom.ranking;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.ranking.dto.MyRankResponse;
import com.studyroom.ranking.dto.RankingEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ranking", description = "누적 이용시간 랭킹 (Redis Sorted Set)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/rankings")
public class RankingController {

	private final RankingService rankingService;

	public RankingController(RankingService rankingService) {
		this.rankingService = rankingService;
	}

	@Operation(summary = "랭킹 목록", description = "scope=all|daily, 상위 limit명 (기본 20, 최대 100).")
	@GetMapping
	public List<RankingEntryResponse> top(
			@RequestParam(defaultValue = "all") String scope,
			@RequestParam(defaultValue = "20") int limit) {
		return rankingService.top(RankingScope.from(scope), limit);
	}

	@Operation(summary = "내 순위", description = "랭크에 없으면 rank=null.")
	@GetMapping("/me")
	public MyRankResponse myRank(
			@AuthenticationPrincipal MemberPrincipal principal,
			@RequestParam(defaultValue = "all") String scope) {
		return rankingService.myRank(RankingScope.from(scope), principal.memberId());
	}

	@Operation(summary = "랭킹 재구축", description = "ADMIN. usage_logs 로 Sorted Set을 다시 만든다 (Redis 유실 복구).")
	@PostMapping("/rebuild")
	public ResponseEntity<Void> rebuild() {
		rankingService.rebuild();
		return ResponseEntity.noContent().build();
	}
}
