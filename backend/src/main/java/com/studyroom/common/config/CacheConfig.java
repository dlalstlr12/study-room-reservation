package com.studyroom.common.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * Redis 캐시. 캐시마다 성격이 달라 TTL을 따로 준다.
 *
 * <ul>
 *   <li>{@link #ROOMS} — 룸 목록·상세. 거의 안 바뀌므로 길게(10분), 변경 시 전체 무효화.</li>
 *   <li>{@link #ROOM_SCHEDULE} — 룸별 하루 현황. 예약·홀딩으로 자주 바뀌므로 짧게(30초)
 *       + 변경 지점마다 전체 무효화(무효화 지점이 많아 정밀 키 관리보다 단순함이 낫다).</li>
 * </ul>
 *
 * {@code spring.cache.type=none} 으로 띄우면 캐시 없이 동작한다(캐시 전/후 부하 비교용).
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String ROOMS = "rooms";
	public static final String ROOM_SCHEDULE = "room-schedule";

	@Bean
	public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
		return builder -> builder
				.withCacheConfiguration(ROOMS, RedisCacheConfiguration.defaultCacheConfig()
						.entryTtl(Duration.ofMinutes(10))
						.disableCachingNullValues())
				.withCacheConfiguration(ROOM_SCHEDULE, RedisCacheConfiguration.defaultCacheConfig()
						.entryTtl(Duration.ofSeconds(30))
						.disableCachingNullValues());
	}
}
