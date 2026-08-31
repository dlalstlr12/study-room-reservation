package com.studyroom.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservationCreateRequest(
		@NotNull Long roomId,
		@NotNull @Future LocalDateTime startAt,
		@NotNull @Future LocalDateTime endAt
) {
}
