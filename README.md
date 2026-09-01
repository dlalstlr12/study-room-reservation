# 스터디룸 예약 시스템

예약 + 이벤트 추첨 + 알림 + 실시간 랭킹 + 정기 구독권을 하나의 도메인으로 묶은 백엔드 포트폴리오 프로젝트입니다.

전체 설계와 개발 로드맵은 [`docs/roadmap.md`](./docs/roadmap.md)를 참고하세요.

## 구조

```
study-room-reservation/
├── backend/   # Spring Boot 3 (Java 17) — Gradle Kotlin DSL
├── frontend/  # React + TypeScript (Vite)
└── docs/      # 설계 문서 (로드맵, 추후 트러블슈팅 기록 등)
```

## 왜 React + TypeScript(Vite)인가

이 프로젝트의 중심은 백엔드(동시성 제어, 캐싱, 메시징, 배치)이고 프론트는 기능을 검증·시연하는 역할입니다. Next.js는 SSR/라우팅 등 백엔드와 무관한 설정이 늘어나 포트폴리오의 초점을 흐릴 수 있어 제외했습니다. Vite + React + TypeScript 조합은,

- 설정이 가볍고 개발 서버 기동이 빨라 백엔드 API·WebSocket 연동 확인에 집중하기 좋고
- 예약 현황판(WebSocket 실시간 갱신), 랭킹 보드, 당첨자 발표처럼 **상태가 자주 바뀌는 화면**을 다루기에 React의 컴포넌트/상태 모델이 자연스러우며
- TypeScript로 백엔드 DTO와 타입을 맞춰가는 과정 자체가 API 설계 실력을 보여주는 요소가 됩니다.

## 로컬 실행

### 1. 인프라 (MySQL, Redis)
```bash
docker compose up -d
```

### 2. 백엔드
```bash
cd backend
./gradlew bootRun
```
> Windows PowerShell에서는 `.\gradlew bootRun`으로 실행하고, `&&` 대신 명령을 줄 단위로 나눠 실행하세요. Gradle Wrapper(8.10.2)는 `backend/gradle/wrapper/`에 포함돼 있어 별도 설치가 필요 없습니다.

### 3. 프론트엔드
```bash
cd frontend
npm install
npm run dev
```

## 초기 상태 확인

- 백엔드 헬스체크: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- 프론트: http://localhost:5173

## 프론트엔드 (단계별 기능 시연)

각 로드맵 단계에서 추가된 백엔드 기능을 화면에서 직접 확인하는 데모 앱입니다.
백엔드 주소는 `frontend/.env`의 `VITE_API_BASE_URL`(기본 `http://localhost:8080`)로 지정합니다.

| 경로 | 내용 |
|---|---|
| `/` 대시보드 | 백엔드 헬스 상태, 룸/내 예약 통계, 단계별 기능 링크 |
| `/signup` `/login` | 회원가입·로그인, 토큰은 localStorage 저장 후 401 시 리프레시로 자동 재발급 |
| `/rooms` | 룸 목록 (공개). ADMIN은 생성·수정·삭제 |
| `/rooms/:id` | 룸 예약 현황 타임라인(날짜별 예약·홀딩), 30분 슬롯 홀딩 → 카운트다운 → 확정 |
| `/reservations` | (로그인 필요) 내 홀딩(확정/해제)·내 예약 목록·취소 |

> CORS: 백엔드는 `app.cors.allowed-origins`(기본 `http://localhost:5173,http://localhost:5174`)만 허용합니다.

## API (로드맵 1단계 — 코어 도메인)

DB 스키마는 Flyway(`backend/src/main/resources/db/migration`)가 관리하고, `local` 프로파일에서는
`LocalDataInitializer`가 데모 데이터를 시드합니다.

- **데모 관리자 계정**: `admin@studyroom.local` / `admin1234` (룸 생성·수정·삭제 권한)

| 메서드 | 경로 | 권한 |
|---|---|---|
| POST | `/api/auth/signup` | 공개 |
| POST | `/api/auth/login` | 공개 — accessToken/refreshToken 발급 |
| POST | `/api/auth/reissue` | 공개 — 리프레시 토큰 회전 |
| POST | `/api/auth/logout` | 인증 |
| GET | `/api/members/me` | 인증 |
| GET | `/api/rooms`, `/api/rooms/{id}` | 공개 |
| GET | `/api/rooms/{id}/schedule?date=` | 공개 — 날짜별 예약·홀딩 현황 |
| POST/PUT/DELETE | `/api/rooms`, `/api/rooms/{id}` | ADMIN |
| POST | `/api/reservations` | 인증 (동시성 제어 — 아래 참고) |
| GET | `/api/reservations/me` | 인증 |
| GET | `/api/reservations/{id}` | 인증 (본인 또는 ADMIN) |
| POST | `/api/reservations/{id}/cancel` | 인증 (본인) |
| POST | `/api/reservations/holds` | 인증 — 좌석 홀딩 (TTL 10분) |
| GET | `/api/reservations/holds/me` | 인증 |
| POST | `/api/reservations/holds/{roomId}/{holdId}/confirm` | 인증 — 홀딩 → 예약 확정 |
| DELETE | `/api/reservations/holds/{roomId}/{holdId}` | 인증 — 홀딩 해제 |

### Swagger로 인증 테스트

1. `POST /api/auth/login`으로 토큰을 받습니다.
2. Swagger UI 우측 상단 **Authorize**에 `accessToken` 값을 붙여넣습니다(접두어 `Bearer` 제외).
3. 인증이 필요한 엔드포인트를 그대로 실행합니다.

> `jwt.secret`은 `application-local.yml`에 개발용으로만 들어 있습니다(최소 32바이트).
> 운영 배포 시 환경변수 등으로 반드시 교체하세요.

## 동시성 제어 (로드맵 2단계)

락 없는 예약 생성에서 오버부킹(같은 룸·겹치는 시간에 예약 여러 건)이 재현됐다.
`reservation.lock.strategy`로 전략을 전환하며 해결·비교했다. 전 과정: [`docs/troubleshooting.md`](./docs/troubleshooting.md).

| 전략 | 오버부킹 | 처리량(req/s) | p95 |
|---|---|---|---|
| `none` | **20건 (버그)** | 477 | 18ms |
| `pessimistic` (기본값) | 1건 | 344 | 63ms |
| `distributed` (Redisson) | 1건 | 143 | 155ms |

- 재현/검증: `backend/src/test/java/com/studyroom/reservation/concurrency/` (Testcontainers)
- 부하테스트: `docker run --rm -i grafana/k6 run - < backend/load-test/reservation-conflict.js`

## 캐싱 · 홀딩 (로드맵 3단계)

락으로도 "룸 선택 후 확정까지의 몇 분"은 못 잡는다. **Redis TTL 홀딩**(10분)으로 확정 유예를 주고,
만료는 **keyspace 이벤트 + 스케줄러 백스톱**으로 처리한다(이벤트는 신뢰성 보장 X). 룸 목록·현황은
**Redis 캐싱**. 전 과정: [`docs/troubleshooting.md`](./docs/troubleshooting.md).

| 대상 | 캐시 없음 | Redis 캐싱 |
|---|---|---|
| `GET /rooms` + `/rooms/{id}/schedule` (30 VU) | p95 68ms · 738 req/s | p95 34ms · 1,700 req/s |

- 예약·홀딩 시간은 30분 슬롯 고정. 룸 페이지는 룸 클릭 → 예약 현황 타임라인.
- 재현/검증: `backend/src/test/java/com/studyroom/reservation/hold/`, `.../schedule/`, `.../common/cache/`
- 부하테스트: `backend/load-test/holding-rush.js`, `backend/load-test/room-read.js`

## 다음 단계

로드맵 4단계(실시간): WebSocket/STOMP로 좌석 상태(예약·홀딩·만료) 브로드캐스트.
