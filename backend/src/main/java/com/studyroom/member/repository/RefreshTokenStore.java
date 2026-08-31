package com.studyroom.member.repository;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 리프레시 토큰을 Redis 에 보관한다. 회원당 한 개(최근 발급본)만 유지되며,
 * TTL 로 자동 만료되고 로그아웃 시 즉시 삭제된다.
 */
@Repository
public class RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh:";

	private final StringRedisTemplate redisTemplate;

	public RefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void save(Long memberId, String refreshToken, Duration ttl) {
		redisTemplate.opsForValue().set(key(memberId), refreshToken, ttl);
	}

	public Optional<String> find(Long memberId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
	}

	public void delete(Long memberId) {
		redisTemplate.delete(key(memberId));
	}

	private String key(Long memberId) {
		return KEY_PREFIX + memberId;
	}
}
