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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 추첨 통합 테스트 공통 픽스처. 실제 MySQL·Redis 컨테이너에 붙는다. */
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

	@BeforeEach
	void setUpRoom() {
		sharedRoom = roomRepository.save(Room.create("추첨-테스트-" + UUID.randomUUID(), 20, null));
	}

	protected Long newMember() {
		Member m = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(m).getId();
	}

	/** 해당 회원에게 [start, end) RESERVED 예약을 만든다. */
	protected void reserve(Long memberId, LocalDateTime start, LocalDateTime end) {
		Member member = memberRepository.findById(memberId).orElseThrow();
		reservationRepository.save(Reservation.create(member, sharedRoom, start, end));
	}

	/** 내일 정시. */
	protected LocalDateTime tomorrowAt(int hour) {
		return LocalDateTime.now().plusDays(1).truncatedTo(java.time.temporal.ChronoUnit.DAYS)
				.plusHours(hour);
	}
}
