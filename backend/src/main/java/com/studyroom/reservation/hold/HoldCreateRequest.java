package com.studyroom.reservation.hold;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record HoldCreateRequest(
		@NotNull Long roomId,
		@NotNull @Future LocalDateTime startAt,
		@NotNull @Future LocalDateTime endAt
) {
}
