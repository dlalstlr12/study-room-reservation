package com.studyroom.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.member.entity.Member;
import com.studyroom.ranking.dto.RankingEntryResponse;
import com.studyroom.ranking.message.UsageEventMessage;
import com.studyroom.support.RankingScenarioSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 랭킹 조회·재구축 로직. */
class RankingServiceTest extends RankingScenarioSupport {

	@Autowired
	RankingService rankingService;

	@Test
	@DisplayName("top 은 점수순 + 회원 이름 매핑, 내 순위는 랭크 없으면 null")
	void top_and_my_rank() {
		Long a = memberRepository.save(Member.create("a-" + System.nanoTime() + "@t.local", "x", "앨리스")).getId();
		Long b = memberRepository.save(Member.create("b-" + System.nanoTime() + "@t.local", "x", "밥")).getId();
		rankingRepository.add(RankingScope.ALL, null, a, 120);
		rankingRepository.add(RankingScope.ALL, null, b, 60);

		List<RankingEntryResponse> top = rankingService.top(RankingScope.ALL, 10);
		assertThat(top).extracting(RankingEntryResponse::memberName).containsExactly("앨리스", "밥");
		assertThat(top).extracting(RankingEntryResponse::rank).containsExactly(1, 2);

		assertThat(rankingService.myRank(RankingScope.ALL, a).rank()).isEqualTo(1);
		assertThat(rankingService.myRank(RankingScope.ALL, 999_999L).rank()).isNull();
	}

	@Test
	@DisplayName("rebuild 는 usage_logs 합계로 Sorted Set 을 복원한다")
	void rebuild_from_usage_logs() {
		Long memberId = newMember();
		LocalDateTime now = LocalDateTime.now();
		usageLogRepository.save(UsageLog.of(new UsageEventMessage(101L, memberId, 1L, 40, now)));
		usageLogRepository.save(UsageLog.of(new UsageEventMessage(102L, memberId, 1L, 25, now)));

		redis.delete(RankingRepository.ALL_KEY); // Redis 유실 시뮬레이션
		assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId)).isZero();

		rankingService.rebuild();

		assertThat(rankingRepository.scoreOf(RankingScope.ALL, null, memberId)).isEqualTo(65);
	}
}
