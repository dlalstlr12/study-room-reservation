package com.studyroom.support;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.ranking.RankingRepository;
import com.studyroom.ranking.UsageLogRepository;
import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 랭킹 통합 테스트 공통 픽스처. 실제 MySQL·Redis·Kafka 컨테이너에 붙는다.
 * 랭킹 Sorted Set 은 컨테이너 전역이라 각 테스트가 끝날 때 자기 키를 비운다.
 */
public abstract class RankingScenarioSupport extends IntegrationTest {

	@Autowired
	protected RankingRepository rankingRepository;
	@Autowired
	protected UsageLogRepository usageLogRepository;
	@Autowired
	protected ReservationRepository reservationRepository;
	@Autowired
	protected RoomRepository roomRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected StringRedisTemplate redis;
	@Autowired
	protected PasswordEncoder passwordEncoder;

	@AfterEach
	void clearRanking() {
		var keys = redis.keys("ranking:*");
		if (keys != null && !keys.isEmpty()) {
			redis.delete(keys);
		}
		usageLogRepository.deleteAll();
	}

	protected Room newRoom() {
		return roomRepository.save(Room.create("랭킹-테스트-" + UUID.randomUUID(), 4, null));
	}

	protected Long newMember() {
		Member m = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(m).getId();
	}

	/** {@code [start, end)} 구간의 RESERVED 예약. */
	protected Reservation reservation(Long memberId, Room room, LocalDateTime start, LocalDateTime end) {
		Member member = memberRepository.findById(memberId).orElseThrow();
		return reservationRepository.save(Reservation.create(member, room, start, end));
	}
}
