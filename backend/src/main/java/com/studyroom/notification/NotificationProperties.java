package com.studyroom.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 알림 설정. {@code notification.*}
 *
 * @param delivery 발송 게이트웨이 동작 (장애 시뮬레이션 확률)
 * @param retry    {@code @RetryableTopic} 재시도 파라미터 (SpEL로 참조)
 */
@ConfigurationProperties("notification")
public record NotificationProperties(Delivery delivery, Retry retry) {

	public NotificationProperties {
		if (delivery == null) {
			delivery = new Delivery(0.0);
		}
		if (retry == null) {
			retry = new Retry(4, 500L, 2.0);
		}
	}

	/**
	 * @param failureRate 발송 시 예외를 던질 확률 (0.0~1.0). 재시도·DLT 시연·측정용.
	 */
	public record Delivery(double failureRate) {
		public Delivery {
			if (failureRate < 0.0) {
				failureRate = 0.0;
			}
			if (failureRate > 1.0) {
				failureRate = 1.0;
			}
		}
	}

	/**
	 * @param attempts          원본 1회 + 재시도 포함 총 시도 횟수
	 * @param backoffMs         첫 재시도 지연(ms)
	 * @param backoffMultiplier 재시도마다 지연에 곱할 배수
	 */
	public record Retry(int attempts, long backoffMs, double backoffMultiplier) {
	}
}
