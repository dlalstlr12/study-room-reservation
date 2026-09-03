package com.studyroom.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ADMIN 전체 공지 발송 요청. */
public record AnnouncementRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 1000) String body
) {
}
