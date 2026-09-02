package com.studyroom.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.common.realtime.RoomChangeEvent;
import com.studyroom.reservation.hold.HoldResponse;
import com.studyroom.support.HoldScenarioSupport;
import com.studyroom.support.StompTestClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * 룸 현황이 바뀌면 {@code /topic/rooms/{roomId}} 구독자에게 즉시 알림이 간다.
 * 구독자는 폴링 없이 이벤트 순간에만 현황을 다시 조회한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomChangeBroadcastTest extends HoldScenarioSupport {

	private static final Logger log = LoggerFactory.getLogger(RoomChangeBroadcastTest.class);

	@LocalServerPort
	int port;

	@Test
	@DisplayName("홀딩·해제가 그 룸 구독자에게 브로드캐스트된다")
	void hold_and_release_are_broadcast() throws Exception {
		long roomId = newRoom().getId();
		Long memberId = newMember();

		StompSession session = StompTestClient.connect(port);
		BlockingQueue<RoomChangeEvent> events =
				StompTestClient.subscribe(session, "/topic/rooms/" + roomId, RoomChangeEvent.class);

		HoldResponse hold = holdService.hold(memberId, holdRequest(roomId, 10, 1));

		RoomChangeEvent first = events.poll(3, TimeUnit.SECONDS);
		assertThat(first).isNotNull();
		assertThat(first.roomId()).isEqualTo(roomId);
		assertThat(first.actorMemberId()).isEqualTo(memberId);

		holdService.release(memberId, roomId, hold.holdId());

		RoomChangeEvent second = events.poll(3, TimeUnit.SECONDS);
		assertThat(second).isNotNull();
		assertThat(second.roomId()).isEqualTo(roomId);

		session.disconnect();
	}

	@Test
	@DisplayName("다른 룸 구독자에게는 가지 않는다")
	void other_room_subscribers_untouched() throws Exception {
		long roomA = newRoom().getId();
		long roomB = newRoom().getId();

		StompSession session = StompTestClient.connect(port);
		BlockingQueue<RoomChangeEvent> bEvents =
				StompTestClient.subscribe(session, "/topic/rooms/" + roomB, RoomChangeEvent.class);

		holdService.hold(newMember(), holdRequest(roomA, 10, 1));

		assertThat(bEvents.poll(1, TimeUnit.SECONDS)).isNull();
		session.disconnect();
	}

	@Test
	@DisplayName("구독자 20명 브로드캐스트 지연 측정")
	void fanout_latency() throws Exception {
		long roomId = newRoom().getId();
		int subscribers = 20;

		List<StompSession> sessions = new ArrayList<>();
		List<BlockingQueue<RoomChangeEvent>> queues = new ArrayList<>();
		for (int i = 0; i < subscribers; i++) {
			StompSession s = StompTestClient.connect(port);
			sessions.add(s);
			queues.add(StompTestClient.subscribe(s, "/topic/rooms/" + roomId, RoomChangeEvent.class));
		}

		holdService.hold(newMember(), holdRequest(roomId, 12, 1));

		List<Long> latenciesMs = new ArrayList<>();
		for (BlockingQueue<RoomChangeEvent> q : queues) {
			RoomChangeEvent e = q.poll(3, TimeUnit.SECONDS);
			assertThat(e).isNotNull();
			latenciesMs.add(Duration.between(e.at(), Instant.now()).toMillis());
		}
		latenciesMs.sort(Long::compareTo);
		long p50 = latenciesMs.get(latenciesMs.size() / 2);
		long p95 = latenciesMs.get((int) (latenciesMs.size() * 0.95));
		log.info("[브로드캐스트 지연] 구독자 {}명 · 발행~수신 p50={}ms p95={}ms", subscribers, p50, p95);

		assertThat(p95).isLessThan(500);

		sessions.forEach(StompSession::disconnect);
	}
}
