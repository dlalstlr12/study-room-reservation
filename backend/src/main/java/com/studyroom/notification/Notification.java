package com.studyroom.notification;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 이력 한 건. Kafka 워커가 소비하면서 만든다.
 *
 * <p>{@code dedupKey} 는 at-least-once 재처리에서 중복 저장을 막는 멱등 키다
 * (컬럼 UNIQUE + 저장 전 조회). 발송에 성공하면 {@link NotificationStatus#SENT},
 * 재시도를 모두 소진하면 DLT 핸들러가 {@link NotificationStatus#FAILED} 로 남긴다.
 */
@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 1000)
	private String body;

	/** 관련 엔티티 id (추첨 이벤트 등). 공지처럼 없을 수 있다. */
	@Column(name = "ref_id")
	private Long refId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status;

	@Column(name = "dedup_key", nullable = false, length = 150)
	private String dedupKey;

	private LocalDateTime readAt;

	private Notification(Long memberId, NotificationType type, String title, String body,
			Long refId, String dedupKey, NotificationStatus status) {
		this.memberId = memberId;
		this.type = type;
		this.title = title;
		this.body = body;
		this.refId = refId;
		this.dedupKey = dedupKey;
		this.status = status;
	}

	/** 발송 성공 이력. */
	public static Notification sent(Long memberId, NotificationType type, String title, String body,
			Long refId, String dedupKey) {
		return new Notification(memberId, type, title, body, refId, dedupKey, NotificationStatus.SENT);
	}

	/** 재시도 소진 후 최종 실패 이력. */
	public static Notification failed(Long memberId, NotificationType type, String title, String body,
			Long refId, String dedupKey) {
		return new Notification(memberId, type, title, body, refId, dedupKey, NotificationStatus.FAILED);
	}

	public boolean isRead() {
		return readAt != null;
	}

	public void markRead() {
		if (readAt == null) {
			this.readAt = LocalDateTime.now();
		}
	}
}
