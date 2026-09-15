package com.studyroom.common.config;

import com.studyroom.lottery.LotteryAudience;
import com.studyroom.lottery.LotteryEntry;
import com.studyroom.lottery.LotteryEntryRepository;
import com.studyroom.lottery.LotteryEvent;
import com.studyroom.lottery.LotteryEventRepository;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.notification.Notification;
import com.studyroom.notification.NotificationRepository;
import com.studyroom.notification.NotificationType;
import com.studyroom.ranking.RankingRepository;
import com.studyroom.ranking.RankingScope;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import com.studyroom.subscription.Payment;
import com.studyroom.subscription.PaymentRepository;
import com.studyroom.subscription.Subscription;
import com.studyroom.subscription.SubscriptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발/시연용 시드 데이터. {@code local}·{@code demo}(배포) 프로파일에서 동작하며 멱등하다.
 * 데모 관리자 계정: {@code admin@studyroom.local} / {@code admin1234}
 * (포트폴리오 시연용이므로 계정 정보를 공개해도 무방하다.)
 *
 * <p>예약·랭킹·추첨·알림·구독은 실제 서비스 로직(이벤트 발행·Kafka 소비)을 거치지 않고
 * 리포지토리에 바로 저장한다. 방문자가 빈 화면 대신 "이렇게 채워진다"를 곧바로 볼 수 있으면
 * 충분한 데모용 데이터라, 비동기 파이프라인까지 거칠 필요는 없다.
 */
@Component
@Profile({"local", "demo"})
@ConditionalOnProperty(name = "app.demo-seed.enabled", matchIfMissing = true)
public class LocalDataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(LocalDataInitializer.class);

	private static final String ADMIN_EMAIL = "admin@studyroom.local";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String DEMO_MEMBER_PASSWORD = "demo1234";

	private final MemberRepository memberRepository;
	private final RoomRepository roomRepository;
	private final ReservationRepository reservationRepository;
	private final RankingRepository rankingRepository;
	private final LotteryEventRepository lotteryEventRepository;
	private final LotteryEntryRepository lotteryEntryRepository;
	private final NotificationRepository notificationRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PaymentRepository paymentRepository;
	private final PasswordEncoder passwordEncoder;

	public LocalDataInitializer(MemberRepository memberRepository, RoomRepository roomRepository,
			ReservationRepository reservationRepository, RankingRepository rankingRepository,
			LotteryEventRepository lotteryEventRepository, LotteryEntryRepository lotteryEntryRepository,
			NotificationRepository notificationRepository, SubscriptionRepository subscriptionRepository,
			PaymentRepository paymentRepository, PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.roomRepository = roomRepository;
		this.reservationRepository = reservationRepository;
		this.rankingRepository = rankingRepository;
		this.lotteryEventRepository = lotteryEventRepository;
		this.lotteryEntryRepository = lotteryEntryRepository;
		this.notificationRepository = notificationRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.paymentRepository = paymentRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedAdmin();
		seedRooms();
		seedDemoActivity();
	}

	private void seedAdmin() {
		if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
			return;
		}
		Member admin = Member.createAdmin(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "관리자");
		memberRepository.save(admin);
		log.info("[seed] 데모 관리자 계정 생성: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
	}

	private void seedRooms() {
		if (roomRepository.count() > 0) {
			return;
		}
		roomRepository.save(Room.create("집중룸 A", 1, "1인 집중 학습용 캡슐룸"));
		roomRepository.save(Room.create("스터디룸 B", 4, "화이트보드 완비, 4인 그룹 스터디"));
		roomRepository.save(Room.create("세미나룸 C", 8, "빔프로젝터·대형 모니터, 8인 세미나"));
		roomRepository.save(Room.create("회의룸 D", 6, "화상회의 장비 완비, 6인 회의"));
		log.info("[seed] 데모 룸 4개 생성");
	}

	/** 예약·랭킹·추첨·알림·구독 데모 데이터. 데모 회원 1명이 이미 있으면 건너뛴다. */
	private void seedDemoActivity() {
		String firstDemoEmail = "member1@studyroom.local";
		if (memberRepository.existsByEmail(firstDemoEmail)) {
			return;
		}

		Member member1 = memberRepository.save(
				Member.create(firstDemoEmail, passwordEncoder.encode(DEMO_MEMBER_PASSWORD), "이서준"));
		Member member2 = memberRepository.save(
				Member.create("member2@studyroom.local", passwordEncoder.encode(DEMO_MEMBER_PASSWORD), "박하윤"));
		Member member3 = memberRepository.save(
				Member.create("member3@studyroom.local", passwordEncoder.encode(DEMO_MEMBER_PASSWORD), "김도윤"));

		List<Room> rooms = roomRepository.findAll();
		Room roomA = rooms.get(0);
		Room roomB = rooms.get(1);
		Room roomC = rooms.get(2);
		Room roomD = rooms.get(3);

		seedReservations(member1, member2, member3, roomA, roomB, roomC, roomD);
		seedRanking(member1, member2, member3);
		Long winnerId = seedLotteryAndNotifications(member1, member2, member3);
		seedSubscription(member3);

		log.info("[seed] 데모 회원 3명, 예약/랭킹/추첨/알림/구독 생성 (당첨자 memberId={})", winnerId);
	}

	private void seedReservations(Member member1, Member member2, Member member3,
			Room roomA, Room roomB, Room roomC, Room roomD) {
		// 지난 이용 내역 (퇴실 완료 — "내 예약"과 랭킹 화면에 노출)
		complete(member1, roomA, slot(-3, 10, 0), slot(-3, 11, 0));
		complete(member1, roomB, slot(-1, 14, 0), slot(-1, 15, 30));
		complete(member2, roomC, slot(-2, 9, 0), slot(-2, 10, 0));
		complete(member2, roomD, slot(-4, 16, 0), slot(-4, 17, 0));
		complete(member3, roomA, slot(-5, 13, 0), slot(-5, 14, 0));

		// 다가올 예약 (RESERVED 상태 유지 — 룸 화면에 "예약됨"으로 노출)
		reservationRepository.save(Reservation.create(member1, roomC, slot(1, 10, 0), slot(1, 11, 0)));
		reservationRepository.save(Reservation.create(member2, roomA, slot(2, 15, 0), slot(2, 16, 30)));
		reservationRepository.save(Reservation.create(member3, roomD, slot(1, 9, 0), slot(1, 10, 0)));
	}

	private void complete(Member member, Room room, LocalDateTime startAt, LocalDateTime endAt) {
		Reservation reservation = Reservation.create(member, room, startAt, endAt);
		reservation.complete(endAt);
		reservationRepository.save(reservation);
	}

	/** 오늘 날짜에서 {@code dayOffset}일, 시:분을 더한 30분 단위 시각. */
	private LocalDateTime slot(int dayOffset, int hour, int minute) {
		return LocalDate.now().plusDays(dayOffset).atTime(hour, minute);
	}

	private void seedRanking(Member member1, Member member2, Member member3) {
		LocalDate today = LocalDate.now();
		record Score(Member member, long minutes) {
		}
		for (Score s : List.of(new Score(member1, 150), new Score(member2, 120), new Score(member3, 60))) {
			rankingRepository.add(RankingScope.ALL, null, s.member().getId(), s.minutes());
			rankingRepository.add(RankingScope.DAILY, today, s.member().getId(), s.minutes());
		}
	}

	/** 추첨 이벤트 1건(전체 회원 대상, 이미 추첨됨) + 당첨/미당첨 알림. @return 당첨자 memberId */
	private Long seedLotteryAndNotifications(Member member1, Member member2, Member member3) {
		LotteryEvent event = lotteryEventRepository.save(
				LotteryEvent.create("9월 출석 이벤트", "스타벅스 커피 쿠폰", LotteryAudience.ALL_USERS, 1));
		event.markDrawn(1_000_000_007L);
		lotteryEventRepository.save(event);

		Member winner = member2;
		for (Member candidate : List.of(member1, member2, member3)) {
			LotteryEntry entry = LotteryEntry.of(event, candidate.getId());
			if (candidate.getId().equals(winner.getId())) {
				entry.markWinner();
			}
			lotteryEntryRepository.save(entry);

			boolean won = candidate.getId().equals(winner.getId());
			notificationRepository.save(won
					? Notification.sent(candidate.getId(), NotificationType.LOTTERY_WON,
							"추첨에 당첨되었습니다!", "'" + event.getTitle() + "' 이벤트에서 " + event.getPrize()
									+ "에 당첨되셨습니다.", event.getId(), "seed:lottery:" + event.getId() + ":" + candidate.getId())
					: Notification.sent(candidate.getId(), NotificationType.LOTTERY_LOST,
							"추첨 결과 안내", "'" + event.getTitle() + "' 이벤트 추첨 결과, 이번에는 당첨되지 않았습니다.",
							event.getId(), "seed:lottery:" + event.getId() + ":" + candidate.getId()));
		}
		return winner.getId();
	}

	/** 데모 회원 1명을 PRO 구독 + 결제 완료 상태로 만든다. */
	private void seedSubscription(Member subscriber) {
		int proPriceKrw = 9_900;
		Subscription subscription = subscriptionRepository.save(
				Subscription.subscribePro(subscriber.getId(), proPriceKrw));
		subscription.renew();
		subscriptionRepository.save(subscription);

		Payment payment = paymentRepository.save(
				Payment.succeeded(subscription, "seed:payment:" + subscription.getId()));

		notificationRepository.save(Notification.sent(subscriber.getId(), NotificationType.SUBSCRIPTION_PAID,
				"정기결제가 완료되었습니다", "PRO 구독료 " + proPriceKrw + "원이 결제되었습니다.",
				payment.getId(), "seed:sub-paid:" + payment.getId()));
	}
}
