package com.studyroom.reservation.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 홀딩의 Redis 저장소. 값 하나 + 두 개의 인덱스 SET을 다룬다.
 *
 * <pre>
 * hold:{roomId}:{holdId}         → JSON, TTL = reservation.hold.ttl
 * hold:index:room:{roomId}       → SET&lt;holdId&gt;              (룸별 스캔·백스톱)
 * hold:index:member:{memberId}   → SET&lt;"{roomId}:{holdId}"&gt; (내 홀딩 목록)
 * </pre>
 *
 * 인덱스 항목은 값 키가 사라졌으면(=TTL 만료) 조회 시 그 자리에서 제거한다(lazy).
 * 놓친 항목은 {@link HoldSweepScheduler}가 주기적으로 정리한다.
 */
@Repository
public class HoldRepository {

	private static final Logger log = LoggerFactory.getLogger(HoldRepository.class);
	private static final String VALUE_PREFIX = "hold:";
	private static final String ROOM_INDEX_PREFIX = "hold:index:room:";
	private static final String MEMBER_INDEX_PREFIX = "hold:index:member:";

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public HoldRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public void save(Hold hold, Duration ttl) {
		redis.opsForValue().set(valueKey(hold.roomId(), hold.holdId()), serialize(hold), ttl);
		Duration indexTtl = ttl.plusMinutes(10);

		String roomIndex = ROOM_INDEX_PREFIX + hold.roomId();
		redis.opsForSet().add(roomIndex, hold.holdId());
		redis.expire(roomIndex, indexTtl);

		String memberIndex = MEMBER_INDEX_PREFIX + hold.memberId();
		redis.opsForSet().add(memberIndex, hold.roomId() + ":" + hold.holdId());
		redis.expire(memberIndex, indexTtl);
	}

	public Optional<Hold> find(Long roomId, String holdId) {
		String json = redis.opsForValue().get(valueKey(roomId, holdId));
		return json == null ? Optional.empty() : Optional.of(deserialize(json));
	}

	/** 해당 룸의 활성 홀딩. 만료된 인덱스 항목은 정리한다. */
	public List<Hold> findByRoom(Long roomId) {
		String roomIndex = ROOM_INDEX_PREFIX + roomId;
		Set<String> holdIds = redis.opsForSet().members(roomIndex);
		if (holdIds == null || holdIds.isEmpty()) {
			return List.of();
		}
		List<Hold> result = new ArrayList<>();
		for (String holdId : holdIds) {
			find(roomId, holdId).ifPresentOrElse(result::add,
					() -> redis.opsForSet().remove(roomIndex, holdId));
		}
		return result;
	}

	/** 해당 회원의 활성 홀딩. 만료된 인덱스 항목은 정리한다. */
	public List<Hold> findByMember(Long memberId) {
		String memberIndex = MEMBER_INDEX_PREFIX + memberId;
		Set<String> entries = redis.opsForSet().members(memberIndex);
		if (entries == null || entries.isEmpty()) {
			return List.of();
		}
		List<Hold> result = new ArrayList<>();
		for (String entry : entries) {
			int sep = entry.indexOf(':');
			if (sep < 0) {
				redis.opsForSet().remove(memberIndex, entry);
				continue;
			}
			Long roomId = Long.valueOf(entry.substring(0, sep));
			String holdId = entry.substring(sep + 1);
			find(roomId, holdId).ifPresentOrElse(result::add,
					() -> redis.opsForSet().remove(memberIndex, entry));
		}
		return result;
	}

	public void delete(Hold hold) {
		redis.delete(valueKey(hold.roomId(), hold.holdId()));
		redis.opsForSet().remove(ROOM_INDEX_PREFIX + hold.roomId(), hold.holdId());
		redis.opsForSet().remove(MEMBER_INDEX_PREFIX + hold.memberId(),
				hold.roomId() + ":" + hold.holdId());
	}

	/** 만료 리스너용 — 값 키에서 roomId·holdId만 알 때 룸 인덱스만 정리한다. */
	public void removeFromRoomIndex(Long roomId, String holdId) {
		redis.opsForSet().remove(ROOM_INDEX_PREFIX + roomId, holdId);
	}

	/**
	 * 모든 인덱스를 훑어 값 키가 사라진 항목을 제거한다.
	 *
	 * @return 정리가 발생한 룸 id 집합 (호출자가 해당 룸 캐시를 무효화)
	 */
	public Set<Long> sweep() {
		Set<Long> affectedRooms = new LinkedHashSet<>();
		sweepRoomIndexes(affectedRooms);
		sweepMemberIndexes();
		return affectedRooms;
	}

	private void sweepRoomIndexes(Set<Long> affectedRooms) {
		try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions()
				.match(ROOM_INDEX_PREFIX + "*").count(100).build())) {
			while (cursor.hasNext()) {
				String indexKey = cursor.next();
				Long roomId = Long.valueOf(indexKey.substring(ROOM_INDEX_PREFIX.length()));
				Set<String> holdIds = redis.opsForSet().members(indexKey);
				if (holdIds == null) {
					continue;
				}
				for (String holdId : holdIds) {
					if (Boolean.FALSE.equals(redis.hasKey(valueKey(roomId, holdId)))) {
						redis.opsForSet().remove(indexKey, holdId);
						affectedRooms.add(roomId);
					}
				}
			}
		}
	}

	private void sweepMemberIndexes() {
		try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions()
				.match(MEMBER_INDEX_PREFIX + "*").count(100).build())) {
			while (cursor.hasNext()) {
				String indexKey = cursor.next();
				Set<String> entries = redis.opsForSet().members(indexKey);
				if (entries == null) {
					continue;
				}
				for (String entry : entries) {
					int sep = entry.indexOf(':');
					if (sep < 0) {
						redis.opsForSet().remove(indexKey, entry);
						continue;
					}
					Long roomId = Long.valueOf(entry.substring(0, sep));
					String holdId = entry.substring(sep + 1);
					if (Boolean.FALSE.equals(redis.hasKey(valueKey(roomId, holdId)))) {
						redis.opsForSet().remove(indexKey, entry);
					}
				}
			}
		}
	}

	private String valueKey(Long roomId, String holdId) {
		return VALUE_PREFIX + roomId + ":" + holdId;
	}

	private String serialize(Hold hold) {
		try {
			return objectMapper.writeValueAsString(hold);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "홀딩 직렬화 실패");
		}
	}

	private Hold deserialize(String json) {
		try {
			return objectMapper.readValue(json, Hold.class);
		} catch (Exception e) {
			log.warn("홀딩 역직렬화 실패, 무시: {}", json, e);
			throw new BusinessException(ErrorCode.RESERVATION_HOLD_NOT_FOUND);
		}
	}
}
