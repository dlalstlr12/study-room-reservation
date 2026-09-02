package com.studyroom.lottery.dto;

public enum MyLotteryResult {
	/** 응모되지 않음 (해당 시각 이용 안 함 / 추첨 전) */
	NONE,
	/** 응모됐으나 미당첨 */
	LOST,
	/** 당첨 */
	WON
}
