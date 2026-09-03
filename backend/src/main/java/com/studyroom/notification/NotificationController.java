package com.studyroom.notification;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.notification.dto.AnnouncementRequest;
import com.studyroom.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 조회·읽음, 전체 공지(ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final AnnouncementService announcementService;

	public NotificationController(NotificationService notificationService,
			AnnouncementService announcementService) {
		this.notificationService = notificationService;
		this.announcementService = announcementService;
	}

	@Operation(summary = "내 알림 목록", description = "최근 50건. unreadOnly=true 면 안 읽은 것만.")
	@GetMapping
	public List<NotificationResponse> myNotifications(
			@AuthenticationPrincipal MemberPrincipal principal,
			@RequestParam(defaultValue = "false") boolean unreadOnly) {
		return notificationService.myNotifications(principal.memberId(), unreadOnly);
	}

	@Operation(summary = "안 읽은 알림 수")
	@GetMapping("/unread-count")
	public Map<String, Long> unreadCount(@AuthenticationPrincipal MemberPrincipal principal) {
		return Map.of("count", notificationService.unreadCount(principal.memberId()));
	}

	@Operation(summary = "알림 읽음 처리", description = "본인 알림만.")
	@PostMapping("/{notificationId}/read")
	public ResponseEntity<Void> markRead(@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable Long notificationId) {
		notificationService.markRead(notificationId, principal.memberId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "전체 읽음 처리")
	@PostMapping("/read-all")
	public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal MemberPrincipal principal) {
		notificationService.markAllRead(principal.memberId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "전체 공지 발송", description = "ADMIN. 모든 회원에게 알림을 비동기로 발행한다.")
	@PostMapping("/announcements")
	public ResponseEntity<Void> announce(@Valid @RequestBody AnnouncementRequest request) {
		announcementService.broadcast(request.title(), request.body());
		return ResponseEntity.accepted().build();
	}
}
