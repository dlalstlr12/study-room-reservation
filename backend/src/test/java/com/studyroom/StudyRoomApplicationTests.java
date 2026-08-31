package com.studyroom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 참고: 이 테스트는 스프링 컨텍스트를 완전히 로드하므로
// 로컬에서 docker compose up -d (MySQL, Redis)가 실행 중이어야 통과합니다.
// 2단계부터는 Testcontainers 기반 통합 테스트로 대체/보강할 예정입니다.
@SpringBootTest
class StudyRoomApplicationTests {

	@Test
	void contextLoads() {
	}
}
