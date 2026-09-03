package com.studyroom.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.lottery.LotteryAudience;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.notification.dto.NotificationResponse;
import com.studyroom.support.LotteryScenarioSupport;
import com.studyroom.support.StompTestClient;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * 추첨 완료 → Kafka 워커 → 대상 회원의 {@code /topic/notifications/{memberId}} 로
 * 개인 알림이 새로고침 없이 도착한다 (당첨자는 LOTTERY_WON).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		// 이 테스트 컨텍스트의 워커만 소비하도록 전용 토픽·그룹 — 푸시가 같은 서버(포트)에서 나가야
		// STOMP 클라이언트가 받는다.
		"notification.topic=notification-events-lotteryflowtest",
		"notification.consumer.group-id=notif-lottery-flow-test"
})
class LotteryNotificationFlowTest extends LotteryScenarioSupport {

	@LocalServerPort
	int port;

	@Test
	@DisplayName("draw() → 당첨자에게 LOTTERY_WON 알림 푸시")
	void winner_gets_realtime_notification() throws Exception {
		Long winner = newMember();
		reserveNow(winner);
		Long eventId = lotteryService.createEvent(new LotteryEventCreateRequest(
				"한정 굿즈", "텀블러", LotteryAudience.CURRENT_USERS, 1)).id();

		StompSession session = StompTestClient.connect(port);
		BlockingQueue<NotificationResponse> received = StompTestClient.subscribe(
				session, "/topic/notifications/" + winner, NotificationResponse.class);

		lotteryService.draw(eventId);

		NotificationResponse message = received.poll(15, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(message.type()).isEqualTo(NotificationType.LOTTERY_WON);
		assertThat(message.refId()).isEqualTo(eventId);
		assertThat(message.title()).contains("한정 굿즈");
	}
}
