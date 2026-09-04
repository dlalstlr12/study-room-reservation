package com.studyroom.subscription;

import com.studyroom.notification.NotificationEventPublisher;
import com.studyroom.notification.NotificationType;
import com.studyroom.notification.message.NotificationMessage;
import com.studyroom.subscription.message.SubscriptionEventMessage;
import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 아웃박스가 발행한 결제 이벤트를 회원 알림으로 바꾼다. 6단계 알림 파이프라인
 * ({@code notification-events} → 워커 → {@code notifications} 저장 + WebSocket 푸시)으로 흘려보낸다.
 *
 * <p>아웃박스 재발행(at-least-once)에도 알림이 한 건이도록 dedupKey 를 넣는다.
 */
@Component
public class SubscriptionEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);

	private final NotificationEventPublisher notificationEventPublisher;

	public SubscriptionEventConsumer(NotificationEventPublisher notificationEventPublisher) {
		this.notificationEventPublisher = notificationEventPublisher;
	}

	@KafkaListener(topics = "${subscription.topic:subscription-events}", groupId = "subscription-worker",
			containerFactory = "subscriptionEventKafkaListenerContainerFactory")
	public void handle(SubscriptionEventMessage message) {
		boolean succeeded = SubscriptionEventMessage.PAYMENT_SUCCEEDED.equals(message.eventType());
		String amount = NumberFormat.getInstance(Locale.KOREA).format(message.amountKrw());

		NotificationMessage notification = new NotificationMessage(
				succeeded ? NotificationType.SUBSCRIPTION_PAID : NotificationType.SUBSCRIPTION_PAYMENT_FAILED,
				message.memberId(),
				succeeded ? "정기결제 완료" : "정기결제 실패",
				succeeded
						? "PRO 구독료 " + amount + "원이 결제되었습니다."
						: "PRO 구독료 결제에 실패했습니다. 결제 수단을 확인해 주세요.",
				message.subscriptionId(),
				"payment:" + message.subscriptionId() + ":" + message.occurredAt().toLocalDate()
						+ ":" + message.eventType());

		notificationEventPublisher.publish(notification);
		log.info("[구독] 결제 알림 발행 member={} {}", message.memberId(), message.eventType());
	}
}
