package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.lottery.dto.LotteryEventResponse;
import com.studyroom.support.LotteryScenarioSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LotteryDrawTest extends LotteryScenarioSupport {

	@Test
	@DisplayName("targetAt에 이용 중이던 회원만 응모되고, winnerCount만큼 당첨된다")
	void draw_snapshots_active_members() {
		LocalDateTime target = tomorrowAt(14);
		Long a = newMember();
		Long b = newMember();
		Long c = newMember();
		Long absent = newMember();
		reserve(a, target.minusHours(1), target.plusHours(1)); // 이용 중
		reserve(b, target, target.plusMinutes(30));            // 이용 중 (시작 == target)
		reserve(c, target.minusMinutes(30), target.plusMinutes(30));
		reserve(absent, target.plusHours(2), target.plusHours(3)); // target에 이용 안 함

		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"티타임 추첨", "아메리카노", target, LocalDateTime.now().plusHours(1), 2)).id();

		LotteryEventResponse result = lotteryService.draw(eventId, true);

		assertThat(result.status()).isEqualTo(LotteryEventStatus.DRAWN);
		assertThat(result.entryCount()).isEqualTo(3);          // a, b, c
		assertThat(result.winners()).hasSize(2);
		assertThat(lotteryEventRepository.findById(eventId).orElseThrow().getSeed()).isNotNull();

		Set<Long> entrantIds = lotteryEntryRepository.findByEventId(eventId).stream()
				.map(LotteryEntry::getMemberId).collect(java.util.stream.Collectors.toSet());
		assertThat(entrantIds).containsExactlyInAnyOrder(a, b, c);
	}

	@Test
	@DisplayName("응모자가 없으면 당첨자 없이 DRAWN 처리")
	void draw_with_no_candidates() {
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"빈 추첨", "꽝", tomorrowAt(3), LocalDateTime.now().plusHours(1), 1)).id();

		LotteryEventResponse result = lotteryService.draw(eventId, true);

		assertThat(result.status()).isEqualTo(LotteryEventStatus.DRAWN);
		assertThat(result.entryCount()).isZero();
		assertThat(result.winners()).isEmpty();
	}

	@Test
	@DisplayName("이미 추첨된 이벤트를 수동 추첨하면 409")
	void manual_redraw_rejected() {
		LocalDateTime target = tomorrowAt(10);
		reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"중복 추첨", "상품", target, LocalDateTime.now().plusHours(1), 1)).id();
		lotteryService.draw(eventId, true);

		assertThatThrownBy(() -> lotteryService.draw(eventId, true))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOTTERY_ALREADY_DRAWN);
	}

	@Test
	@DisplayName("저장된 seed로 재계산한 당첨자가 실제 당첨자와 일치한다 (재현성)")
	void winners_are_reproducible_from_seed() {
		LocalDateTime target = tomorrowAt(16);
		for (int i = 0; i < 8; i++) {
			reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		}
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"재현 추첨", "상품", target, LocalDateTime.now().plusHours(1), 3)).id();
		lotteryService.draw(eventId, true);

		LotteryEvent event = lotteryEventRepository.findById(eventId).orElseThrow();
		List<Long> candidateIds = lotteryEntryRepository.findByEventId(eventId).stream()
				.map(LotteryEntry::getMemberId).toList();
		List<Long> recomputed = Lottery.draw(candidateIds, event.getWinnerCount(), event.getSeed());

		Set<Long> persistedWinners = lotteryEntryRepository.findByEventIdAndWinnerTrue(eventId).stream()
				.map(LotteryEntry::getMemberId).collect(java.util.stream.Collectors.toSet());

		assertThat(Set.copyOf(recomputed)).isEqualTo(persistedWinners);
	}
}
