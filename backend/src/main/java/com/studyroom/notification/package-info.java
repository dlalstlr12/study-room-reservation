/**
 * 알림 도메인 (로드맵 6단계).
 *
 * <p>추첨 완료·전체 공지 같은 도메인 이벤트를 커밋 후 Kafka({@code notification-events})로 발행하고,
 * 워커({@link com.studyroom.notification.NotificationConsumer})가 소비해
 * {@link com.studyroom.notification.Notification} 이력을 남기고 WebSocket으로 즉시 푸시한다.
 *
 * <p>{@code dedup_key} 로 at-least-once 재처리의 중복을 막고, 발송 실패는
 * {@code @RetryableTopic} 지수 백오프 재시도 → 소진 시 DLT 로 격리한다.
 * 발행 자체의 유실 방지(트랜잭션 아웃박스)는 8단계에서 다룬다.
 */
package com.studyroom.notification;
