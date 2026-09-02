package com.studyroom.common.realtime;

import java.time.Instant;

/**
 * {@code /topic/rooms/{roomId}} 로 발행되는 실시간 알림. "이 룸의 현황이 바뀌었다"만 알린다.
 * 클라이언트는 이걸 받으면 현황을 다시 조회한다(델타를 싣지 않는다).
 *
 * @param roomId        바뀐 룸
 * @param actorMemberId 변경을 일으킨 회원 (홀딩 만료·백스톱은 {@code null}) — 내가 한 액션이면
 *                      클라이언트가 중복 알림을 생략하는 데 쓴다
 * @param at            발행 시각
 */
public record RoomChangeEvent(Long roomId, Long actorMemberId, Instant at) {

	public static RoomChangeEvent now(Long roomId, Long actorMemberId) {
		return new RoomChangeEvent(roomId, actorMemberId, Instant.now());
	}
}
