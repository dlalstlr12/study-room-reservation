package com.studyroom.room.repository;

import com.studyroom.room.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

	/**
	 * 룸 행에 비관적 쓰기 락(SELECT ... FOR UPDATE)을 건다.
	 * 예약 생성의 겹침 검사 직전에 호출해 같은 룸의 동시 생성을 직렬화한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id")
	Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
