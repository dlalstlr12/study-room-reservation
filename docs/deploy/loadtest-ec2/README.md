# AWS EC2 부하테스트 로그 (로드맵 10단계)

2026-09-07, `docs/performance.md` 의 로컬 수치를 대조하려고 AWS EC2 에서 같은 k6 스크립트를
재실행하고 인스턴스는 삭제했다.

| | |
|---|---|
| 인스턴스 | t3.medium (2 vCPU / 4GB), ap-northeast-2, Ubuntu 24.04 |
| 구성 | `deploy/docker-compose.prod.yml` 인프라 + 백엔드 이미지, k6 도 같은 박스 |
| 시나리오 | 예약 동시성 3종 · 홀딩 · 룸조회 캐시 on/off · 랭킹 · 구독 |
| 정리 | 인스턴스 terminated, 볼륨·SG·키페어 삭제 — 이후 비용 $0 (1회성 ~$0.1) |

파일: `reservation-*.txt`, `holding-rush.txt`, `room-read-*.txt`, `ranking-read.txt`,
`subscription-read.txt` (각 k6 요약), `00-run.log` (드라이버 전체 출력).

## 요약 (로컬 대조는 [`../../performance.md`](../performance.md) "로컬 vs AWS EC2")

- 절대 처리량은 로컬 개발 PC 보다 **낮다** — 2 vCPU 에 앱·인프라·k6 를 다 얹은 탓.
- 락 전략 상대 격차, 캐시 2배 효과 등 **패턴은 로컬과 동일**.
- `none` 오버부킹 로컬 10 → EC2 3 (처리량이 낮으면 레이스 창이 좁다).
