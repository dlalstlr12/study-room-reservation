package com.studyroom.lottery.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record LotteryEventCreateRequest(
		@NotBlank @Size(max = 100) String title,
		@NotBlank @Size(max = 200) String prize,
		@NotNull LocalDateTime targetAt,
		@NotNull @Future LocalDateTime drawAt,
		@Min(1) int winnerCount
) {
}
