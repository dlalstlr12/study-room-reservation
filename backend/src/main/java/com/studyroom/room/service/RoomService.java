package com.studyroom.room.service;

import com.studyroom.common.config.CacheConfig;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.room.dto.RoomCreateRequest;
import com.studyroom.room.dto.RoomResponse;
import com.studyroom.room.dto.RoomUpdateRequest;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

	private final RoomRepository roomRepository;

	public RoomService(RoomRepository roomRepository) {
		this.roomRepository = roomRepository;
	}

	@Cacheable(cacheNames = CacheConfig.ROOMS, key = "'all'")
	public List<RoomResponse> getRooms() {
		return roomRepository.findAll().stream().map(RoomResponse::from).toList();
	}

	@Cacheable(cacheNames = CacheConfig.ROOMS, key = "#roomId")
	public RoomResponse getRoom(Long roomId) {
		return RoomResponse.from(getEntity(roomId));
	}

	public Room getEntity(Long roomId) {
		return roomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	/** roomId → 룸 이름. 존재하지 않는 id는 결과에서 빠진다. */
	public Map<Long, String> getRoomNames(Collection<Long> roomIds) {
		return roomRepository.findAllById(roomIds).stream()
				.collect(Collectors.toMap(Room::getId, Room::getName));
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.ROOMS, allEntries = true)
	public RoomResponse create(RoomCreateRequest request) {
		Room room = Room.create(request.name(), request.capacity(), request.description());
		return RoomResponse.from(roomRepository.save(room));
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.ROOMS, allEntries = true)
	public RoomResponse update(Long roomId, RoomUpdateRequest request) {
		Room room = getEntity(roomId);
		room.update(request.name(), request.capacity(), request.description());
		return RoomResponse.from(room);
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.ROOMS, allEntries = true)
	public void delete(Long roomId) {
		Room room = getEntity(roomId);
		roomRepository.delete(room);
	}
}
