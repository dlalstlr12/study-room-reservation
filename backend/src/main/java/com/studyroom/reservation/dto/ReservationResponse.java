package com.studyroom.reservation.dto;

import com.studyroom.reservation.entity.Reservation;
import com.studyroom.reservation.entity.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationResponse(
		Long id,
		Long roomId,
		String roomName,
		Long memberId,
		LocalDateTime startAt,
		LocalDateTime endAt,
		ReservationStatus status,
		LocalDateTime createdAt
) {
	public static ReservationResponse from(Reservation reservation) {
		return new ReservationResponse(
				reservation.getId(),
				reservation.getRoom().getId(),
				reservation.getRoom().getName(),
				reservation.getMember().getId(),
				reservation.getStartAt(),
				reservation.getEndAt(),
				reservation.getStatus(),
				reservation.getCreatedAt()
		);
	}
}
