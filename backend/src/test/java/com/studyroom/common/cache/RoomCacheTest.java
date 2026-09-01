package com.studyroom.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.common.config.CacheConfig;
import com.studyroom.room.dto.RoomCreateRequest;
import com.studyroom.room.service.RoomService;
import com.studyroom.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

class RoomCacheTest extends IntegrationTest {

	@Autowired
	private RoomService roomService;
	@Autowired
	private CacheManager cacheManager;

	@Test
	@DisplayName("getRooms 결과가 rooms 캐시에 담기고, 룸 생성 시 무효화된다")
	void rooms_cache_populated_and_evicted() {
		roomService.getRooms();
		assertThat(cacheManager.getCache(CacheConfig.ROOMS).get("all")).isNotNull();

		roomService.create(new RoomCreateRequest("캐시-무효화-룸", 2, null));

		assertThat(cacheManager.getCache(CacheConfig.ROOMS).get("all")).isNull();
	}

	@Test
	@DisplayName("getRoom(id) 결과가 id 키로 캐시된다")
	void get_room_cached_by_id() {
		Long roomId = roomService.create(new RoomCreateRequest("상세-캐시-룸", 3, null)).id();

		roomService.getRoom(roomId);

		assertThat(cacheManager.getCache(CacheConfig.ROOMS).get(roomId)).isNotNull();
	}
}
