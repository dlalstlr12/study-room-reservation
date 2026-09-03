package com.studyroom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.support.NotificationScenarioSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 알림 조회·읽음 처리 로직. */
class NotificationServiceTest extends NotificationScenarioSupport {

	@Autowired
	NotificationService notificationService;

	private Notification save(Long memberId, String key) {
		return notificationRepository.save(Notification.sent(
				memberId, NotificationType.ANNOUNCEMENT, "공지", "본문", null, key));
	}

	@Test
	@DisplayName("안 읽은 수는 읽음 처리하면 준다")
	void unread_count_drops_after_read() {
		Long memberId = newMember();
		Notification a = save(memberId, uniqueKey());
		save(memberId, uniqueKey());

		assertThat(notificationService.unreadCount(memberId)).isEqualTo(2);

		notificationService.markRead(a.getId(), memberId);
		assertThat(notificationService.unreadCount(memberId)).isEqualTo(1);

		notificationService.markAllRead(memberId);
		assertThat(notificationService.unreadCount(memberId)).isZero();
	}

	@Test
	@DisplayName("남의 알림은 읽음 처리할 수 없다")
	void cannot_read_others_notification() {
		Long owner = newMember();
		Long other = newMember();
		Notification n = save(owner, uniqueKey());

		assertThatThrownBy(() -> notificationService.markRead(n.getId(), other))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("unreadOnly=true 는 안 읽은 것만 반환")
	void unread_only_filter() {
		Long memberId = newMember();
		Notification read = save(memberId, uniqueKey());
		save(memberId, uniqueKey());
		notificationService.markRead(read.getId(), memberId);

		assertThat(notificationService.myNotifications(memberId, true)).hasSize(1);
		assertThat(notificationService.myNotifications(memberId, false)).hasSize(2);
	}
}
