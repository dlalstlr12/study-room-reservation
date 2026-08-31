package com.studyroom.room.dto;

import com.studyroom.room.entity.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomUpdateRequest(
		@NotBlank @Size(max = 100) String name,
		@Positive @Max(100) int capacity,
		@Size(max = 500) String description,
		@NotNull RoomStatus status
) {
}
