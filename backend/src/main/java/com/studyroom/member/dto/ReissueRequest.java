package com.studyroom.member.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
		@NotBlank String refreshToken
) {
}
