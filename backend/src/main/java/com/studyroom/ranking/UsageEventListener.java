package com.studyroom.ranking;

import com.studyroom.ranking.message.UsageEventMessage;
import com.studyroom.reservation.ReservationCompletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 퇴실이 커밋되면 이용 이벤트를 Kafka로 넘긴다. AFTER_COMMIT이라 퇴실 트랜잭션이 롤백되면
 * 랭킹이 오염되지 않는다 (5·6단계와 같은 원칙).
 */
@Component
public class UsageEventListener {

	private final UsageEventPublisher publisher;

	public UsageEventListener(UsageEventPublisher publisher) {
		this.publisher = publisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCompleted(ReservationCompletedEvent event) {
		publisher.publish(new UsageEventMessage(
				event.reservationId(), event.memberId(), event.roomId(),
				event.minutes(), event.occurredAt()));
	}
}
