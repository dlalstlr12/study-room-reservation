package com.studyroom.support;

import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.notification.NotificationEventPublisher;
import com.studyroom.notification.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 알림 통합 테스트 공통 픽스처. 실제 MySQL·Redis·Kafka 컨테이너에 붙는다.
 * Kafka 컨슈머 그룹({@code notification-worker})은 컨테이너 전체에서 하나뿐이므로,
 * 각 테스트는 고유한 dedupKey를 써서 서로의 메시지를 구분한다.
 */
public abstract class NotificationScenarioSupport extends IntegrationTest {

	@Autowired
	protected NotificationEventPublisher notificationEventPublisher;
	@Autowired
	protected NotificationRepository notificationRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;

	@AfterEach
	void clearNotifications() {
		notificationRepository.deleteAll();
	}

	protected Long newMember() {
		Member member = Member.create(UUID.randomUUID() + "@test.local", passwordEncoder.encode("x"), "회원");
		return memberRepository.save(member).getId();
	}

	/** 이 테스트만의 dedupKey 접두사. */
	protected String uniqueKey() {
		return "test:" + UUID.randomUUID();
	}
}
