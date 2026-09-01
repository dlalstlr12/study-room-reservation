package com.studyroom.reservation.hold;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis keyspace 만료 이벤트({@code __keyevent@*__:expired}) 구독자.
 * 홀딩 값 키가 TTL로 사라지면 즉시 룸 인덱스를 정리한다.
 *
 * <p>이 이벤트는 신뢰성이 보장되지 않는다(Redis 문서: 앱이 죽어 있으면 유실, 지연 가능).
 * 놓친 항목은 {@link HoldSweepScheduler}가 주기적으로 맞춘다.
 */
@Component
public class HoldExpirationListener implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(HoldExpirationListener.class);

	private final HoldRepository holdRepository;
	private final HoldExpiryHandler expiryHandler;

	public HoldExpirationListener(HoldRepository holdRepository, HoldExpiryHandler expiryHandler) {
		this.holdRepository = holdRepository;
		this.expiryHandler = expiryHandler;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String key = new String(message.getBody(), StandardCharsets.UTF_8);
		if (!key.startsWith("hold:") || key.startsWith("hold:index:")) {
			return;
		}
		String[] parts = key.split(":", 3); // ["hold", roomId, holdId]
		if (parts.length != 3) {
			return;
		}
		try {
			Long roomId = Long.valueOf(parts[1]);
			holdRepository.removeFromRoomIndex(roomId, parts[2]);
			expiryHandler.onHoldExpired(roomId);
			log.debug("홀딩 만료 처리: room={} hold={}", roomId, parts[2]);
		} catch (NumberFormatException e) {
			log.warn("만료 키 파싱 실패: {}", key);
		}
	}
}
