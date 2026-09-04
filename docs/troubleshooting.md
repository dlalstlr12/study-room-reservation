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

---

## 3단계 — 홀딩과 캐싱

### 문제 1 — 락으로도 못 막는 "고르는 시간"

2단계 락은 "동시에 1명만 성공"을 보장한다. 하지만 실제 서비스에서는 룸을 고르고 결제까지 몇 분이
걸린다. 락 수명은 DB 트랜잭션에 묶여 있어 그 몇 분을 잡아둘 수 없다. 여러 명이 같은 시간대를
고르면 결제 화면까지 갔다가 확정 시점에 1명 빼고 전부 409 — 시간 낭비다.

**해결 — Redis TTL 홀딩**: 먼저 고른 사람에게 `hold:{roomId}:{holdId}` 키(TTL 10분)로 확정 유예를
준다. 나머지는 즉시 `RESERVATION_HOLD_CONFLICT`(409)로 빠른 실패. 예약 생성은 이제
`홀딩 → 확정(RESERVED)` 2단계다.

- 홀딩·확정 모두 `reservation.lock.strategy`(2단계)와 무관하게 **항상 Redisson 룸 락** 안에서
  예약 겹침 + 다른 홀딩 겹침을 함께 검사한다(홀딩은 Redis 전용 개념이라 락도 Redis로 통일).
- 예약·홀딩 시간은 **30분 슬롯**으로 고정(`SlotValidator`) — 프론트 타임라인을 눈금으로 그릴 수
  있고 겹침 계산이 단순해진다.
- k6(`holding-rush.js`, 20 VU × 30s, 같은 슬롯): 홀딩 성공 **1건**, 나머지 409, 5xx 0,
  p95 ≈ 143ms.

### 문제 2 — 홀딩만 하고 이탈하면 자리가 안 풀린다

홀딩 키는 TTL로 사라지지만, **앱은 "그 키가 사라졌다"를 모른다**. Redis는 키를 조용히 지운다.
앱이 알아야 인덱스를 정리하고 룸 현황 캐시를 비운다.

**keyspace 만료 이벤트 — 그리고 그 한계**: `notify-keyspace-events Ex`를 켜면 Redis가
`__keyevent@*__:expired` 채널에 만료된 키 이름을 발행한다(`RedisKeyspaceConfig`가 앱 시작 시
프로그램으로 설정 — compose/Testcontainers 커맨드를 안 건드려도 된다). `HoldExpirationListener`가
구독해 즉시 반응한다.

그런데 Redis 문서는 이 이벤트가 **신뢰성 있는 전달이 아니라고** 명시한다 — 앱이 그 순간 죽어
있었으면 유실되고, 만료 처리가 지연될 수도 있다.

**해결 — 이벤트 + 백스톱 스윕(2단)**: `HoldSweepScheduler`가 1분마다 `hold:index:*`를 훑어
값 키가 사라진 항목을 정리한다. 이벤트가 즉시성을, 스케줄러가 최종 정합성을 담당한다.
인덱스 항목은 조회 시에도 값이 없으면 그 자리에서 제거한다(lazy).

- 검증: `HoldExpiryTest`(TTL 2초로 오버라이드) — 만료 후 같은 슬롯 재홀딩 가능, raw 인덱스가
  결국 빈다.

### 문제 3 — 룸 목록·현황판 반복 조회

현황판(프론트 타임라인)은 룸 목록과 룸별 예약현황을 자주 폴링한다. 매번 `SELECT`.

**해결 — Spring Cache + Redis**, 캐시마다 성격에 맞춰 TTL을 나눴다.

| 캐시 | TTL | 무효화 |
|---|---|---|
| `rooms` (목록·상세) | 10분 | ADMIN 룸 CRUD 시 전체 |
| `room-schedule` (룸별 하루) | 30초 | 예약 생성/취소/확정, 홀딩 생성/해제/만료 시 전체 |

`room-schedule`은 무효화 지점이 5곳(룸 CRUD·예약 2·홀딩 3)이라 룸/날짜별 정밀 키 관리 대신
**전체 비우기 + 짧은 TTL**을 택했다. 무효화는 `RoomScheduleCache.evictAll()` 한 곳으로 모아
호출한다. `mine` 플래그는 뷰어마다 다르므로 **캐시에는 뷰어-무관 데이터만 담고** `mine`은
캐시 밖에서 계산한다(사용자별로 캐시가 쪼개지지 않게).

**수치** (k6 `room-read.js`, 30 VU × 30s, `GET /rooms` + `GET /rooms/{id}/schedule`):

| 구성 | p95 | 처리량(req/s) | 실패 |
|---|---|---|---|
| 캐시 없음 (`spring.cache.type=none`) | 68ms | 738 | 0 |
| Redis 캐싱 | 34ms | 1,700 | 0 |

### redisson JCache와의 자동설정 충돌

`redisson-spring-boot-starter`가 JCache `CachingProvider`를 함께 올려서, Spring Boot 캐시
자동설정이 `RedisCacheManager` 대신 JCache를 골라 `@Cacheable`이 깨졌다
(`getCache("rooms")` → `IllegalArgumentException`). `spring.cache.type=redis`를 명시해 해결.

### 룸 상태(status) 컬럼 제거

`Room.status`(AVAILABLE/HOLDING/OCCUPIED)는 룸 단위 단일 값인데, 이 도메인은 시간대 단위 예약이라
"룸이 지금 무슨 상태인가"를 값 하나로 말할 수 없다(다음 주 슬롯을 홀딩해도 룸 전체가 HOLDING?).
`V2` 마이그레이션으로 컬럼을 걷어내고, 특정 시점 가용성은 **예약 겹침 + 활성 홀딩**으로 판단하며
룸 페이지는 "룸 → 클릭 → 그 룸의 예약 현황 타임라인"으로 바꿨다.

재현/검증 코드: `backend/src/test/java/com/studyroom/reservation/hold/`,
`.../reservation/schedule/`, `.../common/cache/`.
부하 스크립트: `backend/load-test/holding-rush.js`, `backend/load-test/room-read.js`.

---

## 4단계 — 실시간 좌석 상태 브로드캐스트

### 문제 — 현황판이 stale하다

3단계까지 룸 현황(`GET /api/rooms/{id}/schedule`)은 **내가 액션하거나 새로고침할 때만** 갱신된다.
다른 사람이 같은 시간대를 홀딩/예약해도 내 타임라인은 모르고, 확정 버튼을 눌러서야 409를 만난다.

폴링으로 메꾸면? 간격이 짧으면 서버 부하, 길면 여전히 stale. 좌석 현황은 "바뀌는 순간"이
드문드문이라 폴링과 특히 안 맞는다.

### 해결 — 변경 지점에서 WebSocket 브로드캐스트

3단계에서 룸 현황을 바꾸는 모든 지점은 이미 한 곳(`RoomChangeNotifier`, 3단계엔
`RoomScheduleCache.evictAll()` 직접 호출이었다)으로 모여 있었다. 여기에 발행을 얹는다.

```java
public void roomChanged(Long roomId, Long actorMemberId) {
    roomScheduleCache.evictAll();                        // 다음 조회가 최신을 읽도록
    messaging.convertAndSend("/topic/rooms/" + roomId,   // 구독자 즉시 갱신
            RoomChangeEvent.now(roomId, actorMemberId));
}
```

- **알림 → 재조회** 방식. 이벤트는 `{roomId, actorMemberId, at}`만 싣는다. 클라이언트는 델타를
  병합하지 않고 그냥 `schedule.reload()` 한다 — idempotent하고, 캐시가 방금 무효화됐으므로
  DB 조회는 1회뿐. 델타 병합의 구간 매칭 버그를 피한다.
- `actorMemberId`로 "내가 한 액션"이면 클라이언트가 토스트를 생략한다(내 액션 토스트는 이미 봤다).
  홀딩 TTL 만료·백스톱은 `actorMemberId = null`.
- 발행 실패(`convertAndSend`)는 삼켜서 로그만 — 브로드캐스트가 예약/홀딩 트랜잭션을 깨지 않는다.
- STOMP 엔드포인트는 **네이티브 WebSocket**(`/ws`, SockJS 폴백 없음). 브로커는 인메모리
  `SimpleBroker`(`/topic`). 구독은 인증 불필요 — schedule은 이미 REST로 완전 공개다.

### 마주친 것 — 클라이언트/서버 Jackson 모듈 불일치

STOMP 통합 테스트에서 이벤트가 구독자에게 도착하지 않았다. 서버 로그엔 발행 성공, 클라이언트는
조용히 프레임을 버림. `StompSessionHandlerAdapter.handleException`을 구현해 보니:

```
MessageConversionException: Java 8 date/time type `java.time.Instant` not supported by default:
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
```

서버 브로커의 `MappingJackson2MessageConverter`는 Spring Boot가 java.time 모듈이 등록된
ObjectMapper를 주입해줘서 `Instant`를 ISO 문자열로 잘 직렬화했다. 반면 테스트에서
`new MappingJackson2MessageConverter()`로 만든 클라이언트 컨버터는 **맨 ObjectMapper**라
역직렬화에서 터졌다. → 테스트 클라이언트도 `Jackson2ObjectMapperBuilder.json().build()`로 맞춤.
(프론트는 `@stomp/stompjs` + `JSON.parse`라 무관.)

### 수치 — 브로드캐스트 지연

`RoomChangeBroadcastTest.fanout_latency`: 단일 인스턴스, `SimpleBroker`, 같은 JVM에
WebSocket 구독자 20개 연결 → 홀딩 1회 → 각 구독자의 `발행(at) ~ 수신` 지연.

| 구독자 | p50 | p95 |
|---|---|---|
| 20 | ~70ms | ~70ms |

이벤트가 없을 땐 요청 0. 폴링(예: 5초 간격 × 20명 = 240 req/min)과 달리 변경 순간에만
20건의 푸시가 나간다.

재현/검증 코드: `backend/src/test/java/com/studyroom/realtime/`.

---

## 5단계 — 이벤트 추첨

### 문제 1 — `Math.random()` 추첨은 검증할 수 없다

당첨자 시비가 붙으면? 그냥 `Random`으로 뽑으면 결과를 재현할 수도, "조작 없었다"를 증명할 수도
없다.

**해결 — 시드 기록 + 결정적 순서**:

```java
// Lottery.draw
List<Long> ordered = new ArrayList<>(candidateMemberIds);
Collections.sort(ordered);                       // memberId 오름차순 = 결정적 기준 순서
Collections.shuffle(ordered, new Random(seed));  // 시드로 섞기
return ordered.subList(0, min(winnerCount, size));
```

- 시드는 추첨 시점에 `SecureRandom.nextLong()` 으로 뽑아 `lottery_events.seed` 에 기록(사전 예측 불가).
- 후보를 **정렬**해 기준 순서를 고정 → `(후보 집합, seed, winnerCount)` 만 같으면 언제 어디서
  돌려도 같은 당첨자.
- 감사: 저장된 seed + 응모자 목록으로 재계산 → persisted 당첨자와 일치 (`LotteryDrawTest`).
- 추첨 대상은 두 가지: **CURRENT_USERS**(추첨 시점 `RESERVED` 이면서 그 순간이 이용 시간대에
  드는 회원) 또는 **ALL_USERS**(전체 회원). ADMIN이 "지금 추첨"으로 실행한다.

### 문제 2 — 중복 추첨

추첨 버튼 연타, 앱 다중 인스턴스 등으로 같은 이벤트가 두 번 뽑히면 응모자·당첨자가 2배로 저장된다.

**해결 — Redisson 락 + 상태 가드**: `draw()` 는 항상 `lock:lottery:event:{id}` 안에서
`SCHEDULED → DRAWN` 전이를 확인한다. 락에 늦게 들어온 실행은 이미 `DRAWN` 이라
`LOTTERY_ALREADY_DRAWN`(409). `LotteryConcurrencyTest`: 8스레드 동시 `draw()` → 성공 1,
응모자 정확히 1세트.

### 문제 3 — 발표 타이밍

당첨 발표를 추첨 트랜잭션 안에서 바로 WebSocket으로 쏘면, 트랜잭션이 롤백돼도 오발표가 나간다.

**해결 — `@TransactionalEventListener(AFTER_COMMIT)`**: `doDraw()` 는 트랜잭션 안에서
`LotteryDrawnEvent` 를 발행만 하고, `LotteryAnnouncementListener` 가 **커밋된 뒤에만**
`/topic/lottery/{id}` 로 결과를 발표한다. (6단계에서 이 자리에 Kafka 발행 리스너가 붙는다.)

> 처음에 이벤트를 `txTemplate.execute` **밖**에서 발행했더니 활성 트랜잭션이 없어
> `@TransactionalEventListener` 가 아예 발화하지 않았다 — `LotteryBroadcastTest` 로 잡음.

### 수치 — 공정성 분포

`LotteryTest.fair_distribution`: 후보 10명 · 당첨 1명 · 랜덤 시드 10,000회.

| 항목 | 값 |
|---|---|
| 기대 당첨 횟수 / 후보 | 1,000 |
| 실측 최소 | 950 |
| 실측 최대 | 1,038 |

`new Random(seed)` + 정렬된 후보 조합이 균등 분포를 유지한다.

재현/검증 코드: `backend/src/test/java/com/studyroom/lottery/`.

---

## 6단계 — 비동기 알림 (Kafka)

추첨 결과·공지를 개인별 알림으로 남기고 실시간으로 밀어준다. 두 발송 패턴(전체 회원 대상 대량
공지 / 현재 이용중 회원 대상 즉시 알림)을 **추첨 도메인 하나**로 구현한다.

```
추첨 커밋 ─(AFTER_COMMIT)─▶ Kafka: notification-events ─▶ 워커(@KafkaListener)
                                                            ├─ dedup_key 조회 (멱등)
                                                            ├─ 발송 (실패 시 재시도)
                                                            ├─ notifications 이력 (SENT)
                                                            └─ WebSocket /topic/notifications/{id}
     실패 반복 ─▶ -retry-500 ─▶ -retry-1000 ─▶ -retry-2000 ─▶ -dlt ─▶ @DltHandler (이력 FAILED)
```

### 문제 1 — 동기로 전 회원에게 발송하면 추첨이 그 무게를 진다

추첨 트랜잭션 안에서 대상 회원 수만큼 알림을 저장·발송하면, 추첨 응답 시간이 회원 수에 비례해
늘어난다. 더 나쁜 건 **발송 한 건의 실패가 추첨 트랜잭션을 롤백**시킬 수 있다는 것 — 추첨은
끝났는데 결과가 사라진다.

**해결 — 커밋 후 발행, 워커가 소비**: `LotteryNotificationPublisher` 가
`@TransactionalEventListener(AFTER_COMMIT)` 에서 대상별 메시지를 Kafka로 발행만 한다. 추첨은
즉시 커밋·응답하고(발행은 fire-and-forget, 응답시간이 회원 수와 무관), 저장·발송·푸시는 워커의
몫이다. 5단계에서 만든 `@TransactionalEventListener` 자리가 그대로 발행 훅이 됐다.

### 문제 2 — at-least-once 재처리가 알림을 중복시킨다

컨슈머 리밸런스, 재시도, 오프셋 커밋 실패 등으로 같은 메시지가 두 번 처리되면 알림이 두 번
저장되고 두 번 푸시된다.

**해결 — dedup_key**: 메시지마다 `lottery:{eventId}:{memberId}` 같은 멱등 키를 싣고,
워커는 저장 전에 `existsByDedupKey` 로 거른다. `notifications.dedup_key` UNIQUE 제약이 최종
방어선(경합 시 `DataIntegrityViolationException` 을 잡아 "이미 전달됨"으로 처리). 발행이
at-least-once여도 저장은 effectively-once.

### 문제 3 — 외부 발송이 일시적으로 죽으면?

발송 게이트웨이(이메일·푸시)가 잠깐 장애면, 무한 재시도로 컨슈머가 멈추거나 메시지를 버리게 된다.

**해결 — `@RetryableTopic` 논블로킹 재시도 + DLT**: 발송 예외가 나면 메시지를
`notification-events-retry-*` 토픽으로 넘겨 지수 백오프(0.5s → 1s → 2s)로 재시도한다. 메인
파티션은 막히지 않는다. 4회를 모두 소진하면 `notification-events-dlt` 로 격리하고 `@DltHandler`
가 이력을 `FAILED` 로 남긴다 — 운영자가 원인을 고친 뒤 DLT에서 재처리할 수 있다.

### 한계 (다음 단계 예고)

- **발행 자체의 유실**: `AFTER_COMMIT` 뒤 브로커가 죽으면 메시지가 발행되지 못하고 사라진다.
  DB 커밋과 발행을 한 원자 단위로 묶는 **트랜잭션 아웃박스 패턴은 8단계**에서 다룬다.
- **STOMP 세션 미인증**: `/topic/notifications/{memberId}` 는 4·5단계처럼 구독 인증이 없다.
  `/user/queue` + STOMP CONNECT 인증으로의 전환은 후속 과제.

### 마주친 것 — 테스트 컨텍스트끼리 같은 컨슈머 그룹을 공유한다

Testcontainers Kafka는 싱글턴이라 여러 `@SpringBootTest` 컨텍스트가 같은 브로커·같은
`notification-worker` 그룹에 붙는다. 재시도/DLT 테스트가 발송 실패율을 100%로 올려도, 다른
컨텍스트의 정상 워커가 그 메시지를 먼저 집어 성공시켜 버렸다.

**해결**: 컨슈머의 토픽·그룹을 프로퍼티(`notification.topic` / `notification.consumer.group-id`)로
빼고, 재시도/DLT·추첨 알림 E2E 테스트는 각자 전용 토픽·그룹으로 완전히 격리했다. 기본값은 그대로.

### 수치

측정: 로컬 단일 브로커(KRaft), 회원 232명, `AnnouncementService.broadcast` 1회.

| 항목 | 값 |
|---|---|
| 공지 발행 엔드포인트 응답 (p50 / p95) | 15ms / ~170ms |
| ↳ 회원 수 의존성 | 없음 (fire-and-forget 발행) |
| 232건 팬아웃 end-to-end (발행→소비→DB) | ~3.8s |
| 워커 발송 처리량 | ~400 msg/s |
| `failure-rate=0.3` → DLT 유입률 | 관측 0.9% (2/232), 이론값 0.3⁴ ≈ 0.81% |

재현/검증 코드: `backend/src/test/java/com/studyroom/notification/`
(`NotificationConsumerTest` 멱등, `NotificationRetryDltTest` DLT, `LotteryNotificationFlowTest` E2E),
부하 스크립트 `backend/load-test/notification-announce.js`.

---

## 7단계 — 실시간 랭킹 (최장 이용 시간)

퇴실한 이용시간을 회원별로 누적해 순위를 낸다.

```
퇴실(수동 버튼 / 백스톱 스케줄러) ─(AFTER_COMMIT)─▶ Kafka: usage-events ─▶ 랭킹 워커
                                                                          ├─ usage_logs 저장 (reservation_id UNIQUE = 멱등)
                                                                          ├─ ZINCRBY ranking:all
                                                                          └─ ZINCRBY ranking:daily:{yyyy-MM-dd} (TTL 48h)
조회: GET /api/rankings ─▶ ZREVRANGE (DB 집계 없음)
```

### 문제 1 — 랭킹을 DB로 뽑으면 조회가 데이터에 비례한다

```sql
SELECT member_id, SUM(minutes) AS total FROM usage_logs
GROUP BY member_id ORDER BY total DESC LIMIT 20;
```

매 조회가 `usage_logs` 전체를 훑어 그룹·정렬한다. 커버링 인덱스로도 GROUP BY 집계는 행 수에
비례하고, "상위 20"이라는 상한이 스캔량을 줄여주지 못한다. 이용 이력이 쌓일수록 느려진다.

**해결 — Redis Sorted Set**: 갱신은 `ZINCRBY` O(log N), 상위권 조회는 `ZREVRANGE` O(log N + M).
DB(`usage_logs`)는 원천·감사용으로만 남기고, 읽기 경로에서 뺐다.

| 엔드포인트 | 하는 일 | p50 / p95 |
|---|---|---|
| `GET /api/rankings?scope=all&limit=20` | `ZREVRANGE` + 이름 20개 `IN` 조회 | 12ms / 18ms |
| `GET /api/rankings/me` | `ZREVRANK` + `ZSCORE` (순수 Redis) | 4ms / 7ms |

### 문제 2 — 동시에 퇴실이 겹치면 점수가 유실될 수 있다

여러 워커 인스턴스(또는 재시도)가 같은 회원의 점수를 동시에 올릴 때, `GET score → +N → SET`
방식이었다면 마지막 쓰기만 남아 증가분이 사라진다.

**해결 — `ZINCRBY` 는 Redis 단일 명령이라 그 자체가 원자적**. read-modify-write가 아니므로 경쟁해도
정확하다. `RankingConcurrencyTest`: 10 스레드 × 각 20회 `add` → 최종 점수 **정확히 200**.

### 문제 3 — at-least-once 재처리 → 점수 2배

Kafka는 최소 한 번 전달이라 컨슈머 리밸런스·재시도로 같은 이용 이벤트가 두 번 올 수 있다.

**해결 — `usage_logs.reservation_id` UNIQUE**: 워커는 저장에 성공한 경우에만 `ZINCRBY` 한다.
두 번째 처리는 저장이 실패(또는 `existsByReservationId` true)해 집계를 건너뛴다.
`UsageEventConsumerTest`: 같은 `reservationId` 3번 발행 → 점수 1회만 반영.

### 문제 4 — Redis가 비면 랭킹이 사라진다

Sorted Set은 캐시일 뿐이다. Redis 재시작·플러시로 순위가 통째로 날아갈 수 있다.

**해결 — `usage_logs` 가 진실의 원천**: `POST /api/rankings/rebuild`(ADMIN) 가 `SUM(minutes)
GROUP BY member_id` 로 전체 + 오늘 일간 Sorted Set을 다시 만든다. 일간 키는 `ranking:daily:{날짜}`
+ TTL 48h 라 **자정 리셋 배치가 필요 없다** — 어제 키는 알아서 사라진다.

### 설계 메모 — 이용 분 = 예약 구간

수동 퇴실도 예약한 구간(`endAt - startAt`) 전체를 크레딧한다. "퇴실 = 이 예약을 썼다"는 확정으로
보고 이용시간은 예약한 만큼 집계한다. 데모 편의상 예약 시작 전 퇴실도 허용 — 운영에선 `startAt`
도래 후로 제한하거나 실제 경과 시간으로 집계할 여지가 있다(어뷰징 방지). 자동 백스톱
스케줄러(`endAt` 지난 RESERVED → COMPLETED)가 정상 경로다.

### 타입이 다른 두 이벤트 스트림

`notification-events`(6단계)와 `usage-events`가 같은 브로커를 쓴다. 기본 컨슈머는
`NotificationMessage` 를 기본 역직렬화 타입으로 갖고 있어, 랭킹 워커는 `UsageEventMessage` 를 명시한
전용 `ConcurrentKafkaListenerContainerFactory`(`RankingKafkaConfig`)로 분리했다.

재현/검증 코드: `backend/src/test/java/com/studyroom/ranking/`
(`RankingRepositoryTest`, `RankingConcurrencyTest` 원자성, `UsageEventConsumerTest` 멱등,
`ReservationCheckoutTest`/`ReservationCheckoutSchedulerTest` 퇴실 전이),
부하 스크립트 `backend/load-test/ranking-read.js`.
