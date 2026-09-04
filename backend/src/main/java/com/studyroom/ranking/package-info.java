/**
 * 실시간 랭킹 도메인 (로드맵 7단계).
 *
 * <p>퇴실({@link com.studyroom.reservation.ReservationCompletedEvent})을 AFTER_COMMIT으로 구독해
 * Kafka {@code usage-events} 로 발행하고, 랭킹 워커({@link com.studyroom.ranking.UsageEventConsumer})가
 * 소비해 {@link com.studyroom.ranking.UsageLog} 를 남기고 Redis Sorted Set 점수를 {@code ZINCRBY}
 * 한다. 조회는 {@code ZREVRANGE} 로 DB 집계 없이 O(log N).
 *
 * <p>{@code reservation_id} UNIQUE 로 at-least-once 중복 집계를 막고, {@code ZINCRBY} 원자성으로
 * 동시 갱신을 정확히 처리한다. 일간은 날짜 키 + TTL 48h 라 자정 리셋 배치가 필요 없다.
 */
package com.studyroom.ranking;
