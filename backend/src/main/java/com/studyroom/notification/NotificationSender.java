package com.studyroom.notification;

import com.studyroom.notification.message.NotificationMessage;

/**
 * 실제 발송 채널(이메일·푸시 등)의 경계. 이번 단계는 로그 구현체 하나뿐이고,
 * 여기에 장애를 주입해 재시도/DLT를 시연한다.
 */
public interface NotificationSender {

	/** 발송에 실패하면 예외를 던진다 — 워커가 재시도한다. */
	void send(NotificationMessage message);
}
