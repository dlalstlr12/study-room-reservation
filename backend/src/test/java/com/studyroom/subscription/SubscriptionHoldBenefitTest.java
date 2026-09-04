package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.reservation.hold.HoldCreateRequest;
import com.studyroom.reservation.hold.HoldResponse;
import com.studyroom.reservation.hold.HoldService;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/** PRO 구독자가 홀딩하면 유예가 20분(기본 10분)으로 잡힌다 — 도메인 연계. */
class SubscriptionHoldBenefitTest extends SubscriptionScenarioSupport {

	@Autowired
	HoldService holdService;
	@Autowired
	RoomRepository roomRepository;
	@Autowired
	StringRedisTemplate redis;

	@Test
	@DisplayName("PRO 회원 홀딩 → expiresAt ≈ now + 20분")
	void pro_gets_longer_hold() {
		Long pro = newMember();
		subscriptionRepository.save(Subscription.subscribePro(pro, 9900));
		Room room = roomRepository.save(Room.create("구독-홀딩-" + UUID.randomUUID(), 4, null));

		LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS).plusHours(9);
		HoldResponse hold = holdService.hold(pro,
				new HoldCreateRequest(room.getId(), start, start.plusHours(1)));

		long minutesUntilExpiry = Duration.between(LocalDateTime.now(), hold.expiresAt()).toMinutes();
		assertThat(minutesUntilExpiry).isBetween(18L, 20L);

		redis.delete("hold:" + room.getId() + ":" + hold.holdId());
	}
}
