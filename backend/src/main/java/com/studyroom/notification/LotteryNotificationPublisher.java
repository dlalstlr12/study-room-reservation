package com.studyroom.notification;

import com.studyroom.lottery.LotteryDrawnEvent;
import com.studyroom.lottery.LotteryEntry;
import com.studyroom.lottery.LotteryEntryRepository;
import com.studyroom.lottery.LotteryEvent;
import com.studyroom.lottery.LotteryEventRepository;
import com.studyroom.notification.message.NotificationMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 추첨이 커밋되면 대상 회원 전원에게 <b>개인 알림</b>을 발행한다.
 *
 * <p>{@code LotteryAnnouncementListener}(공개 발표 피드 {@code /topic/lottery})와는 별개다 —
 * 이쪽은 회원별 알림 이력·푸시를 만드는 Kafka 발행만 담당한다.
 */
@Component
public class LotteryNotificationPublisher {

	private static final Logger log = LoggerFactory.getLogger(LotteryNotificationPublisher.class);

	private final LotteryEventRepository eventRepository;
	private final LotteryEntryRepository entryRepository;
	private final NotificationEventPublisher publisher;

	public LotteryNotificationPublisher(LotteryEventRepository eventRepository,
			LotteryEntryRepository entryRepository, NotificationEventPublisher publisher) {
		this.eventRepository = eventRepository;
		this.entryRepository = entryRepository;
		this.publisher = publisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDrawn(LotteryDrawnEvent event) {
		Long eventId = event.eventId();
		LotteryEvent lottery = eventRepository.findById(eventId).orElse(null);
		if (lottery == null) {
			return;
		}
		List<NotificationMessage> messages = entryRepository.findByEventId(eventId).stream()
				.map(entry -> toMessage(lottery, entry))
				.toList();
		publisher.publishAll(messages);
		log.info("[알림 발행] 추첨 event={} 대상 {}명", eventId, messages.size());
	}

	private NotificationMessage toMessage(LotteryEvent lottery, LotteryEntry entry) {
		boolean won = entry.isWinner();
		return new NotificationMessage(
				won ? NotificationType.LOTTERY_WON : NotificationType.LOTTERY_LOST,
				entry.getMemberId(),
				won ? "🎉 " + lottery.getTitle() + " 당첨!" : lottery.getTitle() + " 추첨 결과",
				won ? "'" + lottery.getPrize() + "'에 당첨되셨습니다." : "아쉽게 이번엔 당첨되지 않았어요.",
				lottery.getId(),
				"lottery:" + lottery.getId() + ":" + entry.getMemberId());
	}
}
