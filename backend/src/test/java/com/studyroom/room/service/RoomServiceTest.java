package com.studyroom.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.room.dto.RoomCreateRequest;
import com.studyroom.room.dto.RoomResponse;
import com.studyroom.room.dto.RoomUpdateRequest;
import com.studyroom.room.entity.Room;
import com.studyroom.room.repository.RoomRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	@Mock
	private RoomRepository roomRepository;
	@InjectMocks
	private RoomService roomService;

	@Test
	void 룸_생성() {
		RoomCreateRequest request = new RoomCreateRequest("스터디룸 B", 4, "4인용");
		when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

		RoomResponse response = roomService.create(request);

		assertThat(response.name()).isEqualTo("스터디룸 B");
		assertThat(response.capacity()).isEqualTo(4);
	}

	@Test
	void 룸_수정시_필드가_반영된다() {
		Room room = Room.create("old", 2, "old");
		ReflectionTestUtils.setField(room, "id", 1L);
		when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

		RoomResponse response = roomService.update(1L,
				new RoomUpdateRequest("new", 6, "new desc"));

		assertThat(response.name()).isEqualTo("new");
		assertThat(response.capacity()).isEqualTo(6);
		assertThat(response.description()).isEqualTo("new desc");
	}

	@Test
	void 없는_룸_조회시_ROOM_NOT_FOUND() {
		when(roomRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roomService.getRoom(99L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.ROOM_NOT_FOUND);
	}

	@Test
	void 룸_삭제() {
		Room room = Room.create("room", 2, null);
		ReflectionTestUtils.setField(room, "id", 5L);
		when(roomRepository.findById(5L)).thenReturn(Optional.of(room));

		roomService.delete(5L);

		verify(roomRepository).delete(room);
	}
}
