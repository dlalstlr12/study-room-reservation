package com.studyroom.room.controller;

import com.studyroom.room.dto.RoomCreateRequest;
import com.studyroom.room.dto.RoomResponse;
import com.studyroom.room.dto.RoomUpdateRequest;
import com.studyroom.room.entity.RoomStatus;
import com.studyroom.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "스터디룸 조회(공개) 및 관리(ADMIN)")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@Operation(summary = "룸 목록 조회", description = "status 파라미터로 상태 필터링 가능. 인증 불필요.")
	@GetMapping
	public List<RoomResponse> getRooms(@RequestParam(required = false) RoomStatus status) {
		return roomService.getRooms(status);
	}

	@Operation(summary = "룸 상세 조회", description = "인증 불필요.")
	@GetMapping("/{roomId}")
	public RoomResponse getRoom(@PathVariable Long roomId) {
		return roomService.getRoom(roomId);
	}

	@Operation(summary = "룸 생성", description = "ADMIN 권한 필요.")
	@PostMapping
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
		RoomResponse created = roomService.create(request);
		return ResponseEntity.created(URI.create("/api/rooms/" + created.id())).body(created);
	}

	@Operation(summary = "룸 수정", description = "ADMIN 권한 필요.")
	@PutMapping("/{roomId}")
	public RoomResponse update(@PathVariable Long roomId, @Valid @RequestBody RoomUpdateRequest request) {
		return roomService.update(roomId, request);
	}

	@Operation(summary = "룸 삭제", description = "ADMIN 권한 필요.")
	@DeleteMapping("/{roomId}")
	public ResponseEntity<Void> delete(@PathVariable Long roomId) {
		roomService.delete(roomId);
		return ResponseEntity.noContent().build();
	}
}
