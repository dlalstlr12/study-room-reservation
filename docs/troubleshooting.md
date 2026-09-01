# 트러블슈팅

프로젝트를 진행하며 마주친 문제와 해결 과정을 타임라인으로 기록한다.

---

## 2단계 — 예약 오버부킹 (동시성 제어)

### 문제

`POST /api/reservations`는 "같은 룸에 시간대가 겹치는 예약이 있으면 409"를 보장해야 한다.
그런데 1단계 구현은 겹침 검사와 저장 사이에 아무 잠금이 없었다.

```java
// ReservationService (1단계)
if (reservationRepository.existsOverlap(roomId, startAt, endAt)) {  // (1) 검사
    throw new BusinessException(RESERVATION_TIME_CONFLICT);
}
reservationRepository.save(Reservation.create(...));                // (2) 저장
```

**재현** (`NoLockConcurrencyTest`): 50개 스레드가 배리어에서 동시에 출발해 같은 룸·같은 시간대로
예약을 생성하면 —

```
[재현] 50스레드 동시 요청 → 성공 10, 충돌 40 / RESERVED 10건
```

락 하나만 있으면 1건이어야 할 예약이 **10건** 저장됐다. k6 부하테스트에서는 20명이 20건 전부
성공하기도 했다.

### 원인 — check-then-act 레이스

여러 트랜잭션이 (1)을 **거의 동시에** 통과한다. 이 시점에는 아직 아무도 저장하지 않았으므로
`existsOverlap`은 모두에게 `false`를 돌려준다. 그 뒤 각자 (2)를 실행해 겹치는 행을 나란히 넣는다.
"검사한 상태"와 "행동하는 시점의 상태"가 다른 전형적인 경쟁 조건이다.

### 낙관적 락으로는 왜 못 막나

`Reservation`에 `@Version`이 있지만 이 레이스에는 소용이 없다. 낙관적 락은 **같은 행을 동시에
수정**할 때 버전 불일치로 충돌을 잡는다. 여기서는 서로 **다른 새 행 2개를 insert**하므로 충돌할
공유 행이 없다.

DB 제약으로 막으려면 "구간 겹침 금지" 제약이 필요한데, MySQL에는 범위 배제 제약이 없다
(PostgreSQL은 `EXCLUDE USING gist (room_id WITH =, tsrange(start,end) WITH &&)` 가능).
고정 슬롯이면 `UNIQUE(room_id, slot)`로 끝나지만, 이 프로젝트는 1단계에서 자유 구간을 택했다.

→ 실질적 선택지는 **비관적 락**과 **분산 락** 둘.

전략은 `reservation.lock.strategy` (`none | pessimistic | distributed`)로 전환한다.
`ReservationService.create()`가 `검증 → 락 → TransactionTemplate → 겹침 검사 → 저장` 순서를
코드로 드러낸다.

### 해결 1 — DB 비관적 락

겹침 검사 직전에 **룸 행에 `SELECT ... FOR UPDATE`**를 건다. 같은 룸의 예약 생성이 이 트랜잭션
뒤로 줄을 선다.

```java
// RoomRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select r from Room r where r.id = :id")
Optional<Room> findByIdForUpdate(@Param("id") Long id);

// doCreate() 첫 줄
if (lockProperties.strategy() == PESSIMISTIC) {
    roomRepository.findByIdForUpdate(request.roomId()).orElseThrow(...);
}
```

- `PessimisticLockConcurrencyTest`: 동시 50건 중 **정확히 1건**만 예약, RESERVED 1건.
- 트레이드오프: 락을 **트랜잭션이 끝날 때까지** 잡는다. 그동안 DB 커넥션 하나 + 룸 행 락이
  묶여 있어, 룸별 예약이 몰리면 커넥션 풀 경합으로 번진다. 락 수명이 DB 트랜잭션에 붙어 있다.

### 해결 2 — Redisson 분산 락

룸 키(`lock:reservation:room:{roomId}`)에 대해 Redis 분산 락을 잡고, 그 안에서 트랜잭션을 연다.

```java
// RedissonDistributedLock
RLock lock = redissonClient.getLock(key);
if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) throw new BusinessException(RESERVATION_LOCK_TIMEOUT);
try { return action.get(); }
finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
```

- `DistributedLockConcurrencyTest`: 동시 50건 중 **정확히 1건**만 예약.
- `waitTime` 3s 안에 못 잡으면 `RESERVATION_LOCK_TIMEOUT`(409). `leaseTime` 10s가 지나면
  자동 해제되어 홀더가 죽어도 데드락이 안 남는다.
- 트레이드오프: 매 요청이 Redis 왕복(락/해제) 두 번을 더 탄다. 단일 핫키에 몰리면 처리량이
  크게 떨어진다(아래 표). 대신 **DB 트랜잭션이 짧아지고**, 앱이 여러 인스턴스여도 동작한다.
  Redis가 죽으면 예약 생성 전체가 막히는 SPOF다.

### 성능 비교

측정 환경: 로컬(Windows), MySQL 8 · Redis 7 (Docker), 백엔드 단일 인스턴스,
Hikari pool 20, 로깅 WARN. k6 20 VU × 30초, 같은 룸·같은 시간대로만 `POST /api/reservations`.

| 전략 | 오버부킹(RESERVED) | 처리량(req/s) | p95 | 5xx |
|---|---|---|---|---|
| **none** | **20건** (버그) | 477 | 18ms | 19건 (InnoDB 데드락) |
| **pessimistic** | 1건 | 344 | 63ms | 0 |
| **distributed** | 1건 | 143 | 155ms | 0 |

- `none`의 5xx는 동시 `SELECT ... + INSERT`가 InnoDB 갭 락에서 서로 물려 트랜잭션이 강제 롤백된
  것 — 락이 없으면 DB가 자기를 지키려다 일부 요청을 죽인다.
- 이 시나리오는 **단일 핫키**(한 룸·한 시간)라 분산 락에 가장 불리하다. 여러 룸으로 부하가
  분산되면 격차는 줄어든다.

### 결론

- **단일 인스턴스 + 룸 단위 경합**이면 비관적 락이 단순하고 빠르다.
- **앱 다중화**를 전제하면 분산 락. 대신 Redis SPOF, 락 임대시간 튜닝, Redis 왕복 비용을 감수한다.
- 이 프로젝트 기본값은 `pessimistic` (배포 형태가 단일 인스턴스). 3단계에서 홀딩(TTL)을 붙일 때
  Redis 사용이 늘어나면 재검토한다.

재현/검증 코드: `backend/src/test/java/com/studyroom/reservation/concurrency/`,
부하 스크립트: `backend/load-test/reservation-conflict.js`.
