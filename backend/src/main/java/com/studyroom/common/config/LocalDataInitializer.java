package com.studyroom.common.config;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발/시연용 시드 데이터. {@code local} 프로파일에서만 동작하며 멱등하다.
 * 데모 관리자 계정: {@code admin@studyroom.local} / {@code admin1234}
 */
@Component
@Profile("local")
public class LocalDataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(LocalDataInitializer.class);

	private static final String ADMIN_EMAIL = "admin@studyroom.local";
	private static final String ADMIN_PASSWORD = "admin1234";

	private final MemberRepository memberRepository;
	private final RoomRepository roomRepository;
	private final PasswordEncoder passwordEncoder;

	public LocalDataInitializer(MemberRepository memberRepository, RoomRepository roomRepository,
			PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.roomRepository = roomRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedAdmin();
		seedRooms();
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
}
