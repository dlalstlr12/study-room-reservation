package com.studyroom.room.entity;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	private Room(String name, int capacity, String description) {
		this.name = name;
		this.capacity = capacity;
		this.description = description;
	}

	public static Room create(String name, int capacity, String description) {
		return new Room(name, capacity, description);
	}

	public void update(String name, int capacity, String description) {
		this.name = name;
		this.capacity = capacity;
		this.description = description;
	}
}
