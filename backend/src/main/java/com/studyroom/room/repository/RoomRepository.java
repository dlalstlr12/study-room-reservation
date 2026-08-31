package com.studyroom.room.repository;

import com.studyroom.room.entity.Room;
import com.studyroom.room.entity.RoomStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findAllByStatus(RoomStatus status);
}
