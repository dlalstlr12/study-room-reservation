package com.studyroom.lottery;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.lock.DistributedLock;
import com.studyroom.common.lock.RedissonDistributedLock;
import com.studyroom.lottery.dto.LotteryEntryResponse;
import com.studyroom.lottery.dto.LotteryEventCreateRequest;
import com.studyroom.lottery.dto.LotteryEventResponse;
import com.studyroom.lottery.dto.MyLotteryResult;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.reservation.repository.ReservationRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이벤트 추첨. {@code targetAt} 시점에 이용 중이던 회원을 응모시키고, 시드 기반으로 당첨자를 뽑는다.
 *
 * <p>추첨은 항상 Redisson 락 안에서 {@code SCHEDULED → DRAWN} 상태를 가드한다 — 스케줄러 중복
 * 실행이나 다중 인스턴스에서도 정확히 한 번만 뽑힌다.
 */
@Service
public class LotteryService {

	private static final Logger log = LoggerFactory.getLogger(LotteryService.class);
	private static final SecureRandom SEED_SOURCE = new SecureRandom();

	private final LotteryEventRepository eventRepository;
	private final LotteryEntryRepository entryRepository;
	private final ReservationRepository reservationRepository;
	private final MemberRepository memberRepository;
	private final TransactionTemplate txTemplate;
	private final ApplicationEventPublisher eventPublisher;
	private final DistributedLock eventLock;

	public LotteryService(LotteryEventRepository eventRepository, LotteryEntryRepository entryRepository,
			ReservationRepository reservationRepository, MemberRepository memberRepository,
			TransactionTemplate txTemplate, ApplicationEventPublisher eventPublisher,
			RedissonClient redissonClient) {
		this.eventRepository = eventRepository;
		this.entryRepository = entryRepository;
		this.reservationRepository = reservationRepository;
		this.memberRepository = memberRepository;
		this.txTemplate = txTemplate;
		this.eventPublisher = eventPublisher;
		this.eventLock = new RedissonDistributedLock(redissonClient);
	}

	@Transactional
	public LotteryEventResponse createEvent(LotteryEventCreateRequest request) {
		LotteryEvent event = eventRepository.save(LotteryEvent.create(
				request.title(), request.prize(), request.targetAt(), request.drawAt(),
				request.winnerCount()));
		return toResponse(event, List.of(), null);
	}

	/**
	 * 추첨 실행. 수동(ADMIN)·스케줄러 공용.
	 *
	 * @param manual true 면 이미 추첨된 이벤트에 {@link ErrorCode#LOTTERY_ALREADY_DRAWN},
	 *               false(스케줄러) 면 조용히 건너뛴다
	 */
	public LotteryEventResponse draw(Long eventId, boolean manual) {
		eventLock.runWithLock("lock:lottery:event:" + eventId, () ->
				txTemplate.execute(status -> doDraw(eventId, manual)));
		return getEvent(eventId, null);
	}

	private Void doDraw(Long eventId, boolean manual) {
		LotteryEvent event = eventRepository.findById(eventId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LOTTERY_EVENT_NOT_FOUND));
		if (!event.isDrawable()) {
			if (manual) {
				throw new BusinessException(ErrorCode.LOTTERY_ALREADY_DRAWN);
			}
			return null; // 스케줄러 중복 — 이미 다른 실행이 뽑음
		}
		// (아래에서 markDrawn 후 이 트랜잭션 안에서 이벤트 발행 → 커밋되면 AFTER_COMMIT 리스너가 발표)

		List<Long> candidates = reservationRepository.findActiveMemberIdsAt(event.getTargetAt());
		long seed = SEED_SOURCE.nextLong();
		Set<Long> winners = Set.copyOf(Lottery.draw(candidates, event.getWinnerCount(), seed));

		for (Long memberId : candidates) {
			LotteryEntry entry = LotteryEntry.of(event, memberId);
			if (winners.contains(memberId)) {
				entry.markWinner();
			}
			entryRepository.save(entry);
		}
		event.markDrawn(seed);

		log.info("[추첨] event={} seed={} 후보={}명{} 당첨={}", eventId, seed, candidates.size(),
				candidates, winners);
		eventPublisher.publishEvent(new LotteryDrawnEvent(eventId));
		return null;
	}

	@Transactional(readOnly = true)
	public List<LotteryEventResponse> getEvents(Long viewerMemberId) {
		List<LotteryEvent> events = eventRepository.findAllByOrderByDrawAtDesc();
		if (events.isEmpty()) {
			return List.of();
		}
		List<Long> eventIds = events.stream().map(LotteryEvent::getId).toList();
		Map<Long, List<LotteryEntry>> entriesByEvent = entryRepository.findByEventIdIn(eventIds).stream()
				.collect(Collectors.groupingBy(e -> e.getEvent().getId()));
		Map<Long, String> memberNames = memberNames(entriesByEvent.values().stream()
				.flatMap(List::stream).map(LotteryEntry::getMemberId).toList());

		List<LotteryEventResponse> result = new ArrayList<>();
		for (LotteryEvent event : events) {
			List<LotteryEntry> entries = entriesByEvent.getOrDefault(event.getId(), List.of());
			result.add(assemble(event, entries, memberNames, viewerMemberId));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public LotteryEventResponse getEvent(Long eventId, Long viewerMemberId) {
		LotteryEvent event = eventRepository.findById(eventId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LOTTERY_EVENT_NOT_FOUND));
		List<LotteryEntry> entries = entryRepository.findByEventId(eventId);
		Map<Long, String> names = memberNames(entries.stream().map(LotteryEntry::getMemberId).toList());
		return assemble(event, entries, names, viewerMemberId);
	}

	@Transactional(readOnly = true)
	public List<LotteryEntryResponse> myEntries(Long memberId) {
		return entryRepository.findByMemberIdOrderByEventIdDesc(memberId).stream()
				.map(LotteryEntryResponse::from)
				.toList();
	}

	private LotteryEventResponse assemble(LotteryEvent event, List<LotteryEntry> entries,
			Map<Long, String> memberNames, Long viewerMemberId) {
		List<String> winners = entries.stream()
				.filter(LotteryEntry::isWinner)
				.map(e -> memberNames.getOrDefault(e.getMemberId(), "(탈퇴 회원)"))
				.toList();
		MyLotteryResult myResult = MyLotteryResult.NONE;
		if (viewerMemberId != null) {
			myResult = entries.stream()
					.filter(e -> e.getMemberId().equals(viewerMemberId))
					.findFirst()
					.map(e -> e.isWinner() ? MyLotteryResult.WON : MyLotteryResult.LOST)
					.orElse(MyLotteryResult.NONE);
		}
		return LotteryEventResponse.of(event, entries.size(), winners, myResult);
	}

	private LotteryEventResponse toResponse(LotteryEvent event, List<String> winners,
			Long viewerMemberId) {
		return LotteryEventResponse.of(event, 0, winners, MyLotteryResult.NONE);
	}

	private Map<Long, String> memberNames(List<Long> memberIds) {
		if (memberIds.isEmpty()) {
			return Map.of();
		}
		return memberRepository.findAllById(memberIds).stream()
				.collect(Collectors.toMap(Member::getId, Member::getName, (a, b) -> a));
	}
}
