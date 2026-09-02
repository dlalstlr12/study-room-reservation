package com.studyroom.lottery;

/** 추첨이 완료됐을 때 발행되는 in-process 이벤트. AFTER_COMMIT 리스너가 발표를 담당한다. */
public record LotteryDrawnEvent(Long eventId) {
}
