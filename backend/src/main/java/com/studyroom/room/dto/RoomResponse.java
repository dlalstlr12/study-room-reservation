package com.studyroom.room.dto;

import com.studyroom.room.entity.Room;

public record RoomResponse(
		Long id,
		String name,
		int capacity,
		String description
) {
	public static RoomResponse from(Room room) {
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getCapacity(),
				room.getDescription()
		);
	}
}
