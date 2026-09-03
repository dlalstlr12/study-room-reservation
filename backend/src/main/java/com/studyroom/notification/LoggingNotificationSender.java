package com.studyroom.notification;

import com.studyroom.notification.message.NotificationMessage;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 발송 채널 대역. 실제 게이트웨이 대신 로그를 남기되,
 * {@code notification.delivery.failure-rate} 확률로 실패해 재시도·DLT 경로를 만든다.
 */
@Component
public class LoggingNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

	private final NotificationProperties properties;

	public LoggingNotificationSender(NotificationProperties properties) {
		this.properties = properties;
	}

	@Override
	public void send(NotificationMessage message) {
		double failureRate = properties.delivery().failureRate();
		if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
			throw new NotificationDeliveryException("발송 게이트웨이 일시 오류: " + message.dedupKey());
		}
		log.info("[알림 발송] to={} type={} title={}", message.memberId(), message.type(),
				message.title());
	}
}
