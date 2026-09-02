package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.lottery.dto.LotteryEventResponse;
import com.studyroom.support.LotteryScenarioSupport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LotteryDrawTest extends LotteryScenarioSupport {

	private Long createEvent(LotteryAudience audience, int winnerCount) {
		return lotteryService.createEvent(new LotteryEventCreateRequest(
				"추첨", "상품", audience, winnerCount)).id();
	}

	@Test
	@DisplayName("CURRENT_USERS — 지금 이용 중인 회원만 응모, winnerCount만큼 당첨")
	void draw_current_users() {
		Long a = newMember();
		Long b = newMember();
		Long absent = newMember();
		reserveNow(a);
		reserveNow(b);
		// absent 은 예약 없음

		Long eventId = createEvent(LotteryAudience.CURRENT_USERS, 1);
		LotteryEventResponse result = lotteryService.draw(eventId);

		assertThat(result.status()).isEqualTo(LotteryEventStatus.DRAWN);
		assertThat(result.entryCount()).isEqualTo(2);
		assertThat(result.winners()).hasSize(1);

		Set<Long> entrantIds = lotteryEntryRepository.findByEventId(eventId).stream()
				.map(LotteryEntry::getMemberId).collect(Collectors.toSet());
		assertThat(entrantIds).containsExactlyInAnyOrder(a, b);
	}

	@Test
	@DisplayName("ALL_USERS — 전체 회원이 응모")
	void draw_all_users() {
		long before = memberRepository.count();
		newMember();
		newMember();
		newMember();

		Long eventId = createEvent(LotteryAudience.ALL_USERS, 2);
		LotteryEventResponse result = lotteryService.draw(eventId);

		assertThat(result.entryCount()).isEqualTo(before + 3);
		assertThat(result.winners()).hasSize(2);
	}

	@Test
	@DisplayName("CURRENT_USERS 대상인데 이용자가 없으면 당첨자 없이 DRAWN")
	void draw_with_no_current_users() {
		Long eventId = createEvent(LotteryAudience.CURRENT_USERS, 1);
		LotteryEventResponse result = lotteryService.draw(eventId);

		assertThat(result.status()).isEqualTo(LotteryEventStatus.DRAWN);
		assertThat(result.entryCount()).isZero();
		assertThat(result.winners()).isEmpty();
	}

	@Test
	@DisplayName("이미 추첨된 이벤트를 다시 추첨하면 409")
	void redraw_rejected() {
		reserveNow(newMember());
		Long eventId = createEvent(LotteryAudience.CURRENT_USERS, 1);
		lotteryService.draw(eventId);

		assertThatThrownBy(() -> lotteryService.draw(eventId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOTTERY_ALREADY_DRAWN);
	}

	@Test
	@DisplayName("저장된 seed로 재계산한 당첨자가 실제 당첨자와 일치한다 (재현성)")
	void winners_are_reproducible_from_seed() {
		for (int i = 0; i < 8; i++) {
			reserveNow(newMember());
		}
		Long eventId = createEvent(LotteryAudience.CURRENT_USERS, 3);
		lotteryService.draw(eventId);

		LotteryEvent event = lotteryEventRepository.findById(eventId).orElseThrow();
		List<Long> candidateIds = lotteryEntryRepository.findByEventId(eventId).stream()
				.map(LotteryEntry::getMemberId).toList();
		List<Long> recomputed = Lottery.draw(candidateIds, event.getWinnerCount(), event.getSeed());

		Set<Long> persistedWinners = lotteryEntryRepository.findByEventIdAndWinnerTrue(eventId).stream()
				.map(LotteryEntry::getMemberId).collect(Collectors.toSet());

		assertThat(Set.copyOf(recomputed)).isEqualTo(persistedWinners);
	}

	@Test
	@DisplayName("이벤트 삭제 시 응모·당첨 기록도 함께 사라진다")
	void delete_removes_entries() {
		reserveNow(newMember());
		reserveNow(newMember());
		Long eventId = createEvent(LotteryAudience.CURRENT_USERS, 1);
		lotteryService.draw(eventId);
		assertThat(lotteryEntryRepository.countByEventId(eventId)).isEqualTo(2);

		lotteryService.deleteEvent(eventId);

		assertThat(lotteryEventRepository.findById(eventId)).isEmpty();
		assertThat(lotteryEntryRepository.countByEventId(eventId)).isZero();
	}
}
