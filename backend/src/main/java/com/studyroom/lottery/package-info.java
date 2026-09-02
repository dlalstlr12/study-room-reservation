/**
 * 이벤트 추첨 도메인.
 * {@code targetAt} 시점에 이용 중이던(RESERVED) 예약의 회원을 스냅샷으로 응모시키고,
 * 스케줄러가 {@code drawAt} 도래 시 추첨을 실행한다. 추첨 시드를 기록해 결과를 재현·검증할 수 있게 하고,
 * 당첨 발표는 WebSocket({@code /topic/lottery})으로 브로드캐스트한다.
 */
package com.studyroom.lottery;
