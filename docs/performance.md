# 성능 측정 (로드맵 10단계)

각 단계에서 점진적으로 재던 수치를 **한 환경에서 다시 재 정리**한 문서.
단계별 배경·트러블슈팅은 [`troubleshooting.md`](./troubleshooting.md), 여기서는 비교표만 본다.

## 측정 환경

| | |
|---|---|
| 부하 도구 | k6 (Docker), 각 시나리오 30초, 워밍업 없음 |
| 백엔드 | 단일 인스턴스 (`java -jar`, `local` 프로파일, 로깅 WARN), Hikari pool 10 |
| 인프라 | MySQL 8.0 · Redis 7 · Kafka 3.8 (Docker Compose, 로컬) |
| 머신 | 로컬 개발 PC (Windows) — k6·앱·DB 가 같은 머신 |

> **주의**: 개발 머신이라 절대 수치는 실행마다 ±20% 흔들린다. **같은 표 안의 상대 비교**가
> 핵심이고, 운영 유사 스펙(단독 인스턴스) 수치는 별도 EC2 측정으로 보완한다.

---

## 1. 예약 동시성 — 락 전략 비교 ⭐

20 VU 가 30초 동안 **같은 룸·같은 시간대**로만 `POST /api/reservations` (단일 핫키, 락에 최악).

| 전략 | 오버부킹 (RESERVED 행) | 처리량 | p95 | 5xx |
|---|---|---|---|---|
| `none` (락 없음) | **10건** ❌ | 761 req/s | 49 ms | 0 |
| `pessimistic` (DB 비관적 락) | **1건** ✅ | 334 req/s | 83 ms | 0 |
| `distributed` (Redisson 분산 락) | **1건** ✅ | 127 req/s | 205 ms | 0 |

- 락이 없으면 check-then-act 레이스로 **10배 오버부킹**. 나머지는 정확히 1건.
- 비관적 락: 정합성 확보 대가로 처리량 **-56%**, p95 +70%.
- 분산 락: 단일 핫키라 **-83%** (요청마다 Redis 락/해제 왕복 2회). 대신 **앱 다중화 시** DB 락
  경합이 사라져 수평 확장이 된다 — 이 벤치는 단일 인스턴스라 분산 락에 가장 불리한 조건.
- 프로젝트 기본값 `pessimistic` (배포 형태가 단일 인스턴스).

검증 코드 `backend/src/test/.../reservation/concurrency/` · 스크립트 `load-test/reservation-conflict.js`

## 2. 홀딩 러시

20명이 동시에 **같은 슬롯**을 홀딩(`POST /api/reservations/holds`).

| held_201 | 409 (즉시 실패) | 5xx | 처리량 | p95 |
|---|---|---|---|---|
| **1건** | 나머지 전부 | 0 | 101 req/s | 295 ms |

- "확정 단계 몰림"을 **홀딩 앞단의 빠른 실패**로 전환. Redis `SET NX` + TTL 이라 DB 안 감.
- 스크립트 `load-test/holding-rush.js`

## 3. 룸 조회 — 캐시 전/후

30 VU, `GET /api/rooms` + `GET /api/rooms/{id}/schedule` 반복.

| 구성 | 처리량 | p95 | 실패 |
|---|---|---|---|
| Redis 캐싱 | **1,837 req/s** | 36 ms | 0 |
| 캐시 없음 (`spring.cache.type=none`) | 408 req/s | 81 ms | 0 |

- 캐시로 처리량 **4.5배**, p95 **-55%**. 스크립트 `load-test/room-read.js`

## 4. 실시간 랭킹 조회

30 VU, `GET /api/rankings` + `/rankings/me`.

| 처리량 | p95 | 실패 |
|---|---|---|
| 1,423 req/s | 59 ms | 0 |

- Redis Sorted Set 직결(`ZREVRANGE` / `ZREVRANK`+`ZSCORE`) — 이력 규모와 무관하게 O(log N).
  DB `GROUP BY` 집계였다면 `usage_logs` 크기에 비례. 스크립트 `load-test/ranking-read.js`

## 5. 구독 조회

20 VU, `GET /api/subscriptions/me` + `/me/payments`.

| 처리량 | p95 | 실패 |
|---|---|---|
| 871 req/s | 44 ms | 0 |

## 6. 알림 팬아웃 (Kafka)

공지 5건/초 × 30초, 회원 **430명** (공지 1건이 전원에게 개인 알림).

| 항목 | 값 |
|---|---|
| 공지 발행 엔드포인트 p95 | **~27 ms** (fire-and-forget — 팬아웃 규모와 무관) |
| `failure-rate=0.3` → DLT 최종 격리율 | **0.79 %** ≈ 이론값 `0.3⁴` (원본 1회 + 재시도 3회 모두 실패) |
| 워커 처리 | dedup 조회 + DB insert 건별, 컨슈머 동시성 1 → ~80 msg/s |

- 동기 발송이면 엔드포인트가 430건 발송을 전부 기다린다. Kafka 로 떼어내 **발행은 상수 시간**.
- 재시도 백오프(0.5s·1s·2s) 소진 후 `-dlt` 격리, 이력 `FAILED`. 스크립트 `load-test/notification-announce.js`

## 7. 정기결제 배치

`POST /api/admin/billing/run` (Spring Batch, 건별 `REQUIRES_NEW` tx).

| 항목 | 값 |
|---|---|
| 50건 처리 (게이트웨이 + 아웃박스 포함) | ~1.1 s (`BillingJobTest`) |
| 아웃박스 릴레이 드레인 (poll 2s) | 50건 < 4 s |

---

## 로컬 vs 운영 유사 스펙

로컬은 k6·앱·DB 가 CPU 를 나눠 쓰므로 처리량이 눌린다. AWS EC2(t3.medium, 앱 전용) 측정은
`docs/deploy/` 에 별도 기록 예정 — 절대 처리량은 오르고 상대 비교(락 전략 격차 등)는 유지된다.
