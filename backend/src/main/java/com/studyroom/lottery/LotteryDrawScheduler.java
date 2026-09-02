package com.studyroom.lottery;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code drawAt} 이 지난 SCHEDULED 이벤트를 주기적으로 추첨한다.
 * 실제 추첨은 {@link LotteryService#draw}가 락 + 상태 가드로 한 번만 실행되도록 보장한다.
 */
@Component
public class LotteryDrawScheduler {

	private static final Logger log = LoggerFactory.getLogger(LotteryDrawScheduler.class);

	private final LotteryEventRepository eventRepository;
	private final LotteryService lotteryService;

	public LotteryDrawScheduler(LotteryEventRepository eventRepository, LotteryService lotteryService) {
		this.eventRepository = eventRepository;
		this.lotteryService = lotteryService;
	}

	@Scheduled(fixedDelayString = "${lottery.draw.poll-ms:10000}",
			initialDelayString = "${lottery.draw.poll-ms:10000}")
	public void run() {
		var due = eventRepository.findByStatusAndDrawAtLessThanEqual(
				LotteryEventStatus.SCHEDULED, LocalDateTime.now());
		for (LotteryEvent event : due) {
			try {
				lotteryService.draw(event.getId(), false);
				log.info("[추첨 스케줄러] event={} 자동 추첨", event.getId());
			} catch (RuntimeException e) {
				log.warn("[추첨 스케줄러] event={} 실패", event.getId(), e);
			}
		}
	}
}
