package com.studyroom.room.dto;

import com.studyroom.room.entity.Room;
import com.studyroom.room.entity.RoomStatus;

public record RoomResponse(
		Long id,
		String name,
		int capacity,
		String description,
		RoomStatus status
) {
	public static RoomResponse from(Room room) {
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getCapacity(),
				room.getDescription(),
				room.getStatus()
		);
	}
}
