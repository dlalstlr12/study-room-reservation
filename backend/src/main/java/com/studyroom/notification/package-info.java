/**
 * 알림 도메인.
 * 전체 회원 대상(대량, 지연 허용 - 메시지 큐 기반)과
 * 현재 이용중인 회원 대상(소량, 즉시성 - WebSocket 기반) 발송을 분리해서 처리한다.
 * 재시도 정책과 DLQ, 발송 이력 관리를 포함한다.
 */
package com.studyroom.notification;
