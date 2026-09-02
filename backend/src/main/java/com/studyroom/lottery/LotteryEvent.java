package com.studyroom.lottery;

import com.studyroom.common.entity.BaseTimeEntity;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이벤트 추첨 한 건. {@code targetAt} 시점에 이용 중이던(RESERVED) 회원이 응모 대상이 되고,
 * {@code drawAt} 이 지나면 스케줄러가 추첨한다.
 *
 * <p>추첨 시 {@code seed} 를 기록해 언제든 같은 결과를 재현·검증할 수 있게 한다.
 */
@Entity
@Getter
@Table(name = "lottery_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LotteryEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 200)
	private String prize;

	/** 응모 대상 스냅샷 기준 시점 — 이 시각에 이용 중이던 회원이 응모된다. */
	@Column(nullable = false)
	private LocalDateTime targetAt;

	/** 추첨 예정 시각. */
	@Column(nullable = false)
	private LocalDateTime drawAt;

	@Column(nullable = false)
	private int winnerCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LotteryEventStatus status;

	/** 추첨에 사용한 시드 (재현용). 추첨 전엔 null. */
	private Long seed;

	private LocalDateTime drawnAt;

	private LotteryEvent(String title, String prize, LocalDateTime targetAt, LocalDateTime drawAt,
			int winnerCount) {
		this.title = title;
		this.prize = prize;
		this.targetAt = targetAt;
		this.drawAt = drawAt;
		this.winnerCount = winnerCount;
		this.status = LotteryEventStatus.SCHEDULED;
	}

	public static LotteryEvent create(String title, String prize, LocalDateTime targetAt,
			LocalDateTime drawAt, int winnerCount) {
		if (winnerCount < 1) {
			throw new BusinessException(ErrorCode.LOTTERY_INVALID_SCHEDULE, "당첨 인원은 1명 이상이어야 합니다.");
		}
		if (!drawAt.isAfter(LocalDateTime.now())) {
			throw new BusinessException(ErrorCode.LOTTERY_INVALID_SCHEDULE, "추첨 시각은 미래여야 합니다.");
		}
		return new LotteryEvent(title, prize, targetAt, drawAt, winnerCount);
	}

	public boolean isDrawable() {
		return status == LotteryEventStatus.SCHEDULED;
	}

	/** 추첨 완료 처리. 이미 추첨됐으면 예외. */
	public void markDrawn(long seed) {
		if (!isDrawable()) {
			throw new BusinessException(ErrorCode.LOTTERY_ALREADY_DRAWN);
		}
		this.seed = seed;
		this.drawnAt = LocalDateTime.now();
		this.status = LotteryEventStatus.DRAWN;
	}
}
