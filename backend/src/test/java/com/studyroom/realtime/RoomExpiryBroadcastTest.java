package com.studyroom.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.common.realtime.RoomChangeEvent;
import com.studyroom.support.HoldScenarioSupport;
import com.studyroom.support.StompTestClient;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * 홀딩 TTL 만료도 실시간 알림으로 나간다 — 구독자는 아무 액션 없이 자리가 열리는 걸 본다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"reservation.hold.ttl=PT2S", "reservation.hold.sweep-ms=1000"})
class RoomExpiryBroadcastTest extends HoldScenarioSupport {

	@LocalServerPort
	int port;

	@Test
	@DisplayName("TTL 만료 시 actorMemberId=null 이벤트가 브로드캐스트된다")
	void expiry_is_broadcast() throws Exception {
		long roomId = newRoom().getId();

		StompSession session = StompTestClient.connect(port);
		BlockingQueue<RoomChangeEvent> events =
				StompTestClient.subscribe(session, "/topic/rooms/" + roomId, RoomChangeEvent.class);

		holdService.hold(newMember(), holdRequest(roomId, 10, 1)); // 생성 이벤트
		events.poll(3, TimeUnit.SECONDS);

		// 2초 TTL 만료 → 이벤트 리스너/백스톱이 만료 알림 발행
		RoomChangeEvent expiry = events.poll(8, TimeUnit.SECONDS);
		assertThat(expiry).isNotNull();
		assertThat(expiry.roomId()).isEqualTo(roomId);
		assertThat(expiry.actorMemberId()).isNull();

		session.disconnect();
	}
}
