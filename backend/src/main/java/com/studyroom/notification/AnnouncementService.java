package com.studyroom.notification;

import com.studyroom.member.repository.MemberRepository;
import com.studyroom.notification.message.NotificationMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ADMIN 전체 공지. 로드맵 3-3의 "전체 회원 대상 · 대량 · 지연 허용" 패턴 —
 * 회원 수만큼 메시지를 발행하고, 나머지는 워커가 재시도/DLT와 함께 처리한다.
 */
@Service
public class AnnouncementService {

	private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

	private final MemberRepository memberRepository;
	private final NotificationEventPublisher publisher;

	public AnnouncementService(MemberRepository memberRepository,
			NotificationEventPublisher publisher) {
		this.memberRepository = memberRepository;
		this.publisher = publisher;
	}

	public void broadcast(String title, String body) {
		long batchId = System.currentTimeMillis();
		List<Long> memberIds = memberRepository.findAllMemberIds();
		List<NotificationMessage> messages = memberIds.stream()
				.map(memberId -> new NotificationMessage(
						NotificationType.ANNOUNCEMENT, memberId, title, body, null,
						"announce:" + batchId + ":" + memberId))
				.toList();
		publisher.publishAll(messages);
		log.info("[알림 발행] 전체 공지 batch={} 대상 {}명", batchId, messages.size());
	}
}
