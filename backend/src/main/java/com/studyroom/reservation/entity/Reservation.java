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
import java.time.Duration;
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

	/** 퇴실(COMPLETED 전이) 시각. 실제 이용 시간을 이 값으로 잰다. */
	private LocalDateTime checkedOutAt;

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

	/**
	 * 퇴실 처리. {@code RESERVED} 가 아니면 예외 — 수동 퇴실과 백스톱 스케줄러가 같은 예약을
	 * 동시에 건드려도 상태 가드로 한 번만 완료된다.
	 */
	public void complete(LocalDateTime checkOutAt) {
		if (status != ReservationStatus.RESERVED) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_COMPLETABLE);
		}
		this.status = ReservationStatus.COMPLETED;
		this.checkedOutAt = checkOutAt;
	}

	/**
	 * 이용 분 = 예약 구간 전체({@code endAt - startAt}). 퇴실은 "이 예약을 이용했다"는 확정이고,
	 * 이용시간은 예약한 만큼으로 집계한다. (조기 퇴실도 예약 구간으로 크레딧 — 어뷰징 논의는
	 * docs/troubleshooting.md)
	 */
	public int usedMinutes() {
		return (int) Math.max(0, Duration.between(startAt, endAt).toMinutes());
	}
}
