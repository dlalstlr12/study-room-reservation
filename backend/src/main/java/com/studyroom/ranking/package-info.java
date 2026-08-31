/**
 * 실시간 랭킹 도메인.
 * 퇴실 이벤트를 구독해 Redis Sorted Set에 누적 이용시간을 갱신하고,
 * 일간/주간/전체 랭킹을 O(log N) 조회로 제공한다.
 */
package com.studyroom.ranking;
