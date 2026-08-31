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
- 프론트: http://localhost:5173 — 백엔드 헬스체크 상태를 화면에 표시합니다.

## 다음 단계

로드맵 1단계(코어 도메인: 회원 인증, 룸/예약 기본 CRUD, Swagger 문서화)부터 진행합니다.
