package com.studyroom.support;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.reservation.hold.HoldCreateRequest;
import com.studyroom.reservation.hold.HoldRepository;
import com.studyroom.reservation.hold.HoldService;
import com.studyroom.reservation.repository.ReservationRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 홀딩 통합 테스트 공통 픽스처. 실제 MySQL·Redis 컨테이너에 붙는다. */
public abstract class HoldScenarioSupport extends IntegrationTest {

	@Autowired
	protected HoldService holdService;
	@Autowired
	protected HoldRepository holdRepository;
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

	@BeforeEach
	void flushHoldKeys() {
		var keys = redis.keys("hold:*");
		if (keys != null && !keys.isEmpty()) {
			redis.delete(keys);
		}
	}

	protected Room newRoom() {
		return roomRepository.save(Room.create("홀딩-테스트-" + UUID.randomUUID(), 4, null));
	}

	protected Long newMember() {
		Member m = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(m).getId();
	}

	/** 내일 정시로 정렬된 30분 슬롯. */
	protected LocalDateTime tomorrowAt(int hour) {
		return LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS).plusHours(hour);
	}

	protected HoldCreateRequest holdRequest(long roomId, int hour, int durationHours) {
		LocalDateTime start = tomorrowAt(hour);
		return new HoldCreateRequest(roomId, start, start.plusHours(durationHours));
	}
}
