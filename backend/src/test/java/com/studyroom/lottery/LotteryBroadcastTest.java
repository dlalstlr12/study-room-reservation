package com.studyroom.lottery;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.lottery.dto.LotteryResultMessage;
import com.studyroom.support.LotteryScenarioSupport;
import com.studyroom.support.StompTestClient;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

/** 추첨이 커밋되면 /topic/lottery 구독자에게 결과가 발표된다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LotteryBroadcastTest extends LotteryScenarioSupport {

	@LocalServerPort
	int port;

	@Test
	@DisplayName("draw() → /topic/lottery/{id} 로 당첨 결과 브로드캐스트")
	void draw_is_announced() throws Exception {
		LocalDateTime target = tomorrowAt(15);
		reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		reserve(newMember(), target.minusMinutes(30), target.plusMinutes(30));
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"발표 추첨", "커피", target, LocalDateTime.now().plusHours(1), 1)).id();

		StompSession session = StompTestClient.connect(port);
		BlockingQueue<LotteryResultMessage> events = StompTestClient.subscribe(
				session, "/topic/lottery/" + eventId, LotteryResultMessage.class);

		lotteryService.draw(eventId, true);

		LotteryResultMessage message = events.poll(3, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(message.eventId()).isEqualTo(eventId);
		assertThat(message.winners()).hasSize(1);
		assertThat(message.drawnAt()).isNotNull();

		session.disconnect();
	}
}
