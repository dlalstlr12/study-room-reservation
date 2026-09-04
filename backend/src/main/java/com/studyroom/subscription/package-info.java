/**
 * 정기 구독권 도메인 (로드맵 8단계).
 *
 * <p>Spring Batch 일일 잡이 결제일 도래한 구독을 결제한다. 결제·상태변경·아웃박스 저장이
 * <b>한 트랜잭션</b>({@link com.studyroom.subscription.PaymentService})이고, 릴레이
 * ({@link com.studyroom.subscription.OutboxRelay})가 {@link com.studyroom.subscription.OutboxEvent}
 * 를 읽어 Kafka {@code subscription-events} 로 발행한다 — 6·7단계가 남긴 "발행 자체 유실" 틈을 메운다.
 *
 * <p>{@code payments.idempotency_key}(`sub:{id}:{yyyy-MM}`) UNIQUE 로 중복 결제를 막고,
 * PRO 구독자는 홀딩 유예가 연장된다({@link com.studyroom.subscription.SubscriptionBenefit}).
 */
package com.studyroom.subscription;
