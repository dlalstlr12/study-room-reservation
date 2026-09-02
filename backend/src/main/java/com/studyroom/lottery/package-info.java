/**
 * 이벤트 추첨 도메인.
 * 대상은 추첨 시점에 이용 중인 회원({@code CURRENT_USERS}) 또는 전체 회원({@code ALL_USERS}).
 * ADMIN이 추첨을 실행하면 시드를 기록해 결과를 재현·검증할 수 있게 하고, 당첨 발표는
 * WebSocket({@code /topic/lottery})으로 브로드캐스트한다.
 */
package com.studyroom.lottery;
