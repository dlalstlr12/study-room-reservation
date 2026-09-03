package com.studyroom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.studyroom.support.NotificationScenarioSupport;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** ADMIN 전체 공지 → 회원마다 ANNOUNCEMENT 알림이 저장된다. */
class AnnouncementFlowTest extends NotificationScenarioSupport {

	@Autowired
	AnnouncementService announcementService;

	@Test
	@DisplayName("broadcast → 대상 회원 전원에게 SENT 이력")
	void broadcast_fans_out_to_every_member() {
		Set<Long> targets = Set.of(newMember(), newMember(), newMember());

		announcementService.broadcast("서비스 점검", "9/10 02:00~03:00 점검 예정입니다.");

		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
			List<Notification> mine = notificationRepository.findAll().stream()
					.filter(n -> targets.contains(n.getMemberId()))
					.filter(n -> n.getType() == NotificationType.ANNOUNCEMENT)
					.toList();
			assertThat(mine).hasSize(targets.size());
			assertThat(mine).allMatch(n -> n.getStatus() == NotificationStatus.SENT);
			assertThat(mine).allMatch(n -> n.getTitle().equals("서비스 점검"));
		});
	}
}
