package com.studyroom.notification;

/** 발송 채널의 일시적 실패. 워커가 이 예외를 재시도 신호로 삼는다. */
public class NotificationDeliveryException extends RuntimeException {

	public NotificationDeliveryException(String message) {
		super(message);
	}
}
