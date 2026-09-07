# AWS EC2 배포 검증 근거 (로드맵 9단계)

2026-09-07, 전체 스택을 EC2 t3.medium(ap-northeast-2, Ubuntu 24.04)에 `docker compose`로
배포해 동작을 확인하고, 비용 최소화를 위해 인스턴스·볼륨을 완전히 삭제했다.

| 파일 | 내용 |
|---|---|
| `01-caller-identity.json` | `aws sts get-caller-identity` — 배포에 사용한 IAM 사용자 |
| `02-security-group.json` | 보안그룹 인바운드 (SSH=내 IP, 80·8085=공개) |
| `03-instance.json` | `run-instances` 결과 — t3.medium, ap-northeast-2c, 퍼블릭 IP |
| `04-runtime-evidence.txt` | `docker compose ps` · Kafka 컨슈머 그룹(lag 0) · 백엔드 부팅(profile=demo, Flyway 8개, 시드) |
| `05-deploy-summary.txt` | `deploy/deploy.sh` 출력 — 이미지 빌드·컨테이너 헬스·완료 요약 |
| `07-verify-smoke-test.txt` | `deploy/verify.sh` 엔드투엔드 16종 **PASS=16 / FAIL=0** |
| `06-terminated.json` · `06-teardown.json` | 인스턴스 terminated, 볼륨/스냅샷/AMI/키페어/보안그룹 전부 삭제, 이후 비용 $0 |

## 결과 요약

- 헬스 → 회원가입/로그인(JWT) → PRO 구독 → **Spring Batch 정기결제**(`COMPLETED`) →
  결제 이력 반영 → **전체 공지(Kafka)** → 알림 수신(라운드트립) → 랭킹 재구축 전부 정상
- Kafka 토픽 8종(`notification-events` + `-retry-500/1000/2000` + `-dlt`, `subscription-events`,
  `usage-events`, `__consumer_offsets`), 컨슈머 그룹 7개 lag 0
- 가동 ~10분, **1회성 약 $0.01**, 이후 월 청구 $0

## 스크린샷

배포된 `http://<EC2_IP>` 에서 캡처: 대시보드(백엔드 "운영 중"·룸 4개), 관리자 로그인,
관리자 페이지(룸 CRUD·공지), `http://<EC2_IP>:8085` Kafka UI 토픽 목록
(`notification-events` 3건 · `subscription-events` 1건). PNG 는 이 디렉터리에 추가.
