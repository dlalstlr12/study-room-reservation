package com.studyroom.lottery;

import com.studyroom.lottery.dto.LotteryEventResponse;
import com.studyroom.lottery.dto.LotteryResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 추첨이 <b>커밋된 뒤</b> 당첨 결과를 <b>공개 발표 피드</b>({@code /topic/lottery})로 브로드캐스트한다.
 * AFTER_COMMIT이라 추첨 트랜잭션이 롤백되면 오발표가 나가지 않는다.
 *
 * <p>회원별 개인 알림(당첨/미당첨 이력·푸시)은 6단계에서 추가된
 * {@code com.studyroom.notification.LotteryNotificationPublisher}가 Kafka로 발행한다 — 관심사 분리.
 */
@Component
public class LotteryAnnouncementListener {

	private static final Logger log = LoggerFactory.getLogger(LotteryAnnouncementListener.class);

	private final LotteryService lotteryService;
	private final SimpMessagingTemplate messaging;

	public LotteryAnnouncementListener(LotteryService lotteryService, SimpMessagingTemplate messaging) {
		this.lotteryService = lotteryService;
		this.messaging = messaging;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDrawn(LotteryDrawnEvent event) {
		try {
			LotteryEventResponse result = lotteryService.getEvent(event.eventId(), null);
			LotteryResultMessage message = new LotteryResultMessage(
					result.id(), result.title(), result.prize(), result.winners(), result.drawnAt());
			messaging.convertAndSend("/topic/lottery/" + result.id(), message);
			messaging.convertAndSend("/topic/lottery", message);
			log.info("[추첨 발표] event={} 당첨 {}명", result.id(), result.winners().size());
		} catch (RuntimeException e) {
			log.warn("[추첨 발표] 실패 event={}", event.eventId(), e);
		}
	}
}
