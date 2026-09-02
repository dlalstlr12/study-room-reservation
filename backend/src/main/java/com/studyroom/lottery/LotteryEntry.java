package com.studyroom.lottery;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추첨 이벤트에 대한 응모 한 건. 추첨 시점의 스냅샷으로 만들어지며, 당첨자는 {@code winner = true}.
 */
@Entity
@Getter
@Table(name = "lottery_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LotteryEntry extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private LotteryEvent event;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private boolean winner;

	private LotteryEntry(LotteryEvent event, Long memberId) {
		this.event = event;
		this.memberId = memberId;
		this.winner = false;
	}

	public static LotteryEntry of(LotteryEvent event, Long memberId) {
		return new LotteryEntry(event, memberId);
	}

	public void markWinner() {
		this.winner = true;
	}
}
