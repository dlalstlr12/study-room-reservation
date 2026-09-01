package com.studyroom.reservation.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.support.HoldScenarioSupport;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 홀딩만 하고 이탈하면 TTL이 지나 Redis가 값 키를 지운다.
 * 그 뒤 같은 슬롯을 다시 잡을 수 있어야 하고, 인덱스도 결국 비어야 한다
 * (만료 이벤트 또는 백스톱 스윕).
 */
@SpringBootTest(properties = {"reservation.hold.ttl=PT2S", "reservation.hold.sweep-ms=1000"})
class HoldExpiryTest extends HoldScenarioSupport {

	@Test
	@DisplayName("TTL 만료 후 값 키가 사라지고 같은 슬롯을 다시 홀딩할 수 있다")
	void hold_expires_and_slot_frees_up() {
		long roomId = newRoom().getId();
		Long first = newMember();

		HoldResponse hold = holdService.hold(first, holdRequest(roomId, 10, 1));
		assertThat(holdRepository.find(roomId, hold.holdId())).isPresent();

		await().atMost(Duration.ofSeconds(6))
				.untilAsserted(() -> assertThat(holdRepository.find(roomId, hold.holdId())).isEmpty());

		// 다른 회원이 같은 슬롯을 홀딩 가능
		HoldResponse second = holdService.hold(newMember(), holdRequest(roomId, 10, 1));
		assertThat(second.holdId()).isNotEqualTo(hold.holdId());
	}

	@Test
	@DisplayName("만료된 홀딩은 룸 인덱스에서도 결국 사라진다 (만료 이벤트 + 백스톱)")
	void expired_hold_removed_from_index() {
		long roomId = newRoom().getId();
		holdService.hold(newMember(), holdRequest(roomId, 14, 1));

		String indexKey = "hold:index:room:" + roomId;
		assertThat(redis.opsForSet().size(indexKey)).isEqualTo(1);

		// findByRoom을 호출하지 않고 raw 인덱스가 비워지는지 확인 (이벤트/스윕이 정리)
		await().atMost(Duration.ofSeconds(8))
				.untilAsserted(() -> assertThat(redis.opsForSet().size(indexKey)).isZero());
	}
}
