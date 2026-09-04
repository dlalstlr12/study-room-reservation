package com.studyroom.ranking;

import com.studyroom.ranking.message.UsageEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 랭킹 워커. {@code usage-events} 를 소비해 {@code usage_logs} 에 기록하고 Redis Sorted Set 점수를
 * 올린다.
 *
 * <ul>
 *   <li><b>멱등</b>: {@code usage_logs.reservation_id} UNIQUE. 재처리(at-least-once)에서 두 번째
 *       저장이 실패하면 {@code ZINCRBY} 를 건너뛴다 → 점수 중복 없음.</li>
 *   <li><b>원자성</b>: {@code ZINCRBY} 는 Redis 단일 명령이라 여러 워커가 동시에 올려도 정확.</li>
 * </ul>
 *
 * <p>재시도/DLT 는 6단계 {@code NotificationConsumer} 패턴을 그대로 얹을 수 있다 — 이 단계는
 * Sorted Set 집계에 집중하고, Boot 기본 {@code DefaultErrorHandler}(10회 후 skip) 에 맡긴다.
 */
@Component
public class UsageEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(UsageEventConsumer.class);

	private final UsageLogRepository usageLogRepository;
	private final RankingRepository rankingRepository;

	public UsageEventConsumer(UsageLogRepository usageLogRepository,
			RankingRepository rankingRepository) {
		this.usageLogRepository = usageLogRepository;
		this.rankingRepository = rankingRepository;
	}

	@KafkaListener(topics = "${ranking.topic:usage-events}",
			groupId = "${ranking.consumer.group-id:ranking-worker}",
			containerFactory = "usageEventKafkaListenerContainerFactory")
	public void handle(UsageEventMessage message) {
		if (usageLogRepository.existsByReservationId(message.reservationId())) {
			log.debug("[랭킹] 이미 집계됨 reservation={}", message.reservationId());
			return;
		}
		try {
			usageLogRepository.save(UsageLog.of(message));
		} catch (DataIntegrityViolationException duplicate) {
			log.debug("[랭킹] 저장 경합, 중복으로 처리 reservation={}", message.reservationId());
			return;
		}

		rankingRepository.add(RankingScope.ALL, null, message.memberId(), message.minutes());
		rankingRepository.add(RankingScope.DAILY, message.occurredAt().toLocalDate(),
				message.memberId(), message.minutes());
		log.info("[랭킹] +{}분 member={} (reservation={})",
				message.minutes(), message.memberId(), message.reservationId());
	}
}
