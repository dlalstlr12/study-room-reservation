package com.studyroom.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code jwt.*} 설정 바인딩.
 *
 * @param secret               HMAC-SHA 서명 키 (최소 32바이트)
 * @param accessTokenExpireMs  액세스 토큰 만료(ms)
 * @param refreshTokenExpireMs 리프레시 토큰 만료(ms)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		String secret,
		long accessTokenExpireMs,
		long refreshTokenExpireMs
) {
}
