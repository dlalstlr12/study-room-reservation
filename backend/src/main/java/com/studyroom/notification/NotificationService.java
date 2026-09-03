package com.studyroom.notification;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.notification.dto.NotificationResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 알림 조회·읽음 처리. 발송(워커)과 분리된 읽기 쪽 서비스. */
@Service
public class NotificationService {

	private static final int DEFAULT_LIMIT = 50;

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> myNotifications(Long memberId, boolean unreadOnly) {
		PageRequest limit = PageRequest.of(0, DEFAULT_LIMIT);
		List<Notification> notifications = unreadOnly
				? notificationRepository.findByMemberIdAndReadAtIsNullOrderByIdDesc(memberId, limit)
				: notificationRepository.findByMemberIdOrderByIdDesc(memberId, limit);
		return notifications.stream().map(NotificationResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long memberId) {
		return notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
	}

	@Transactional
	public void markRead(Long notificationId, Long memberId) {
		Notification notification = notificationRepository
				.findByIdAndMemberId(notificationId, memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markRead();
	}

	@Transactional
	public void markAllRead(Long memberId) {
		notificationRepository.markAllRead(memberId);
	}
}
