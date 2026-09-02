package com.studyroom.support;

import com.studyroom.lottery.LotteryEntryRepository;
import com.studyroom.lottery.LotteryEventRepository;
import com.studyroom.lottery.LotteryService;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 추첨 통합 테스트 공통 픽스처. 실제 MySQL·Redis 컨테이너에 붙는다.
 *
 * <p>CURRENT_USERS 추첨은 "지금 이용 중"을 컨테이너 전체에서 조회하므로, 이 클래스가 만든
 * "지금" 예약은 각 테스트가 끝날 때 되돌린다(다른 테스트의 스냅샷을 오염시키지 않도록).
 */
public abstract class LotteryScenarioSupport extends IntegrationTest {

	@Autowired
	protected LotteryService lotteryService;
	@Autowired
	protected LotteryEventRepository lotteryEventRepository;
	@Autowired
	protected LotteryEntryRepository lotteryEntryRepository;
	@Autowired
	protected ReservationRepository reservationRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected RoomRepository roomRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;

	private Room sharedRoom;
	private final List<Reservation> createdReservations = new ArrayList<>();

	@BeforeEach
	void setUpRoom() {
		sharedRoom = roomRepository.save(Room.create("추첨-테스트-" + UUID.randomUUID(), 20, null));
		createdReservations.clear();
	}

	@AfterEach
	void cleanUpNowReservations() {
		reservationRepository.deleteAll(createdReservations);
		createdReservations.clear();
	}

	protected Long newMember() {
		Member m = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(m).getId();
	}

	/** 해당 회원이 <b>지금</b> 이용 중인 RESERVED 예약을 만든다 (CURRENT_USERS 대상용). */
	protected void reserveNow(Long memberId) {
		Member member = memberRepository.findById(memberId).orElseThrow();
		LocalDateTime now = LocalDateTime.now();
		createdReservations.add(reservationRepository.save(
				Reservation.create(member, sharedRoom, now.minusHours(1), now.plusHours(1))));
	}
}
