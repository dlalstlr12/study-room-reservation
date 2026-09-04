package com.studyroom.ranking;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.ranking.RankingRepository.MemberScore;
import com.studyroom.ranking.UsageLogRepository.MemberMinutes;
import com.studyroom.ranking.dto.MyRankResponse;
import com.studyroom.ranking.dto.RankingEntryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 랭킹 조회와 재구축. 조회는 Redis Sorted Set 직결(DB 집계 없음), 재구축만 {@code usage_logs} 를 읽는다.
 */
@Service
public class RankingService {

	private static final Logger log = LoggerFactory.getLogger(RankingService.class);
	private static final int MAX_LIMIT = 100;

	private final RankingRepository rankingRepository;
	private final UsageLogRepository usageLogRepository;
	private final MemberRepository memberRepository;

	public RankingService(RankingRepository rankingRepository, UsageLogRepository usageLogRepository,
			MemberRepository memberRepository) {
		this.rankingRepository = rankingRepository;
		this.usageLogRepository = usageLogRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional(readOnly = true)
	public List<RankingEntryResponse> top(RankingScope scope, int limit) {
		int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
		List<MemberScore> scores = rankingRepository.topN(scope, today(scope), capped);
		if (scores.isEmpty()) {
			return List.of();
		}
		Map<Long, String> names = memberNames(scores.stream().map(MemberScore::memberId).toList());

		List<RankingEntryResponse> result = new ArrayList<>();
		int rank = 1;
		for (MemberScore score : scores) {
			result.add(new RankingEntryResponse(rank++, score.memberId(),
					names.getOrDefault(score.memberId(), "(탈퇴 회원)"), score.minutes()));
		}
		return result;
	}

	public MyRankResponse myRank(RankingScope scope, Long memberId) {
		LocalDate date = today(scope);
		return new MyRankResponse(
				rankingRepository.rankOf(scope, date, memberId),
				rankingRepository.scoreOf(scope, date, memberId));
	}

	/** Redis 유실 시 {@code usage_logs} 로 전체 + 오늘 일간 Sorted Set 을 다시 만든다. */
	@Transactional(readOnly = true)
	public void rebuild() {
		LocalDate today = LocalDate.now();
		rankingRepository.clear(RankingScope.ALL, null);
		rankingRepository.clear(RankingScope.DAILY, today);

		for (MemberMinutes row : usageLogRepository.sumByMember()) {
			rankingRepository.add(RankingScope.ALL, null, row.getMemberId(), row.getMinutes());
		}
		LocalDateTime dayStart = today.atStartOfDay();
		for (MemberMinutes row : usageLogRepository.sumByMemberBetween(dayStart, dayStart.plusDays(1))) {
			rankingRepository.add(RankingScope.DAILY, today, row.getMemberId(), row.getMinutes());
		}
		log.info("[랭킹] 재구축 완료 (usage_logs {}건)", usageLogRepository.count());
	}

	private LocalDate today(RankingScope scope) {
		return scope == RankingScope.DAILY ? LocalDate.now() : null;
	}

	private Map<Long, String> memberNames(List<Long> memberIds) {
		return memberRepository.findAllById(memberIds).stream()
				.collect(Collectors.toMap(Member::getId, Member::getName, (a, b) -> a));
	}
}
