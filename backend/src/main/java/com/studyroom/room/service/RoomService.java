package com.studyroom.room.service;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.room.dto.RoomCreateRequest;
import com.studyroom.room.dto.RoomResponse;
import com.studyroom.room.dto.RoomUpdateRequest;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

	private final RoomRepository roomRepository;

	public RoomService(RoomRepository roomRepository) {
		this.roomRepository = roomRepository;
	}

	public List<RoomResponse> getRooms() {
		return roomRepository.findAll().stream().map(RoomResponse::from).toList();
	}

	public RoomResponse getRoom(Long roomId) {
		return RoomResponse.from(getEntity(roomId));
	}

	public Room getEntity(Long roomId) {
		return roomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	@Transactional
	public RoomResponse create(RoomCreateRequest request) {
		Room room = Room.create(request.name(), request.capacity(), request.description());
		return RoomResponse.from(roomRepository.save(room));
	}

	@Transactional
	public RoomResponse update(Long roomId, RoomUpdateRequest request) {
		Room room = getEntity(roomId);
		room.update(request.name(), request.capacity(), request.description());
		return RoomResponse.from(room);
	}

	@Transactional
	public void delete(Long roomId) {
		Room room = getEntity(roomId);
		roomRepository.delete(room);
	}
}
