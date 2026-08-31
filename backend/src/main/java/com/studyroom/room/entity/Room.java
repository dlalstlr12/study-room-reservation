package com.studyroom.room.entity;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private int capacity;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomStatus status;

	private Room(String name, int capacity, String description) {
		this.name = name;
		this.capacity = capacity;
		this.description = description;
		this.status = RoomStatus.AVAILABLE;
	}

	public static Room create(String name, int capacity, String description) {
		return new Room(name, capacity, description);
	}

	public void update(String name, int capacity, String description, RoomStatus status) {
		this.name = name;
		this.capacity = capacity;
		this.description = description;
		this.status = status;
	}
}
