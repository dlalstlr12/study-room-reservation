package com.studyroom.subscription;

/**
 * 결제 게이트웨이(PG)의 경계. 이번 단계는 로그 구현체 하나뿐이고, 여기에 장애를 주입해
 * 재시도·PAST_DUE 흐름을 시연한다.
 */
public interface PaymentGateway {

	/**
	 * @param idempotencyKey PG 쪽 중복 방지 키
	 * @param amountKrw      청구 금액
	 */
	PaymentResult charge(String idempotencyKey, long amountKrw);
}
