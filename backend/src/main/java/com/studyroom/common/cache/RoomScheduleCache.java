package com.studyroom.common.cache;

import com.studyroom.common.config.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 룸 현황 캐시를 비우는 단일 진입점. 예약·홀딩·만료 등 현황을 바꾸는 모든 지점이 이걸 호출한다.
 * 무효화 지점이 많아 룸/날짜별 정밀 무효화 대신 전체 비우기 + 짧은 TTL(30초) 조합을 쓴다.
 */
@Component
public class RoomScheduleCache {

	private final CacheManager cacheManager;

	public RoomScheduleCache(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void evictAll() {
		Cache cache = cacheManager.getCache(CacheConfig.ROOM_SCHEDULE);
		if (cache != null) {
			cache.clear();
		}
	}
}
