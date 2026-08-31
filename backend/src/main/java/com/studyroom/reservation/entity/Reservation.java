package com.studyroom.reservation.entity;

import com.studyroom.common.entity.BaseTimeEntity;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.entity.Member;
import com.studyroom.room.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	/**
	 * 낙관적 락용 버전. 1단계에서는 락 로직을 붙이지 않지만(로드맵 2단계 예정)
	 * 스키마·엔티티는 미리 준비해 둔다.
	 */
	@Version
	private Long version;

	private Reservation(Member member, Room room, LocalDateTime startAt, LocalDateTime endAt) {
		this.member = member;
		this.room = room;
		this.startAt = startAt;
		this.endAt = endAt;
		this.status = ReservationStatus.RESERVED;
	}

	public static Reservation create(Member member, Room room, LocalDateTime startAt, LocalDateTime endAt) {
		return new Reservation(member, room, startAt, endAt);
	}

	public boolean isOwnedBy(Long memberId) {
		return member.getId().equals(memberId);
	}

	public void cancel() {
		if (status != ReservationStatus.RESERVED) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
		}
		this.status = ReservationStatus.CANCELLED;
	}
}
