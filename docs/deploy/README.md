# 배포 검증 근거 (로드맵 9단계)

## A. AWS EC2 1회성 검증

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

## B. 상시 무료 배포 (Render + Vercel + TiDB + Upstash + Confluent)

| 파일 | 내용 |
|---|---|
| `08-verify-render-standing.txt` | Render URL 대상 `deploy/verify.sh` **PASS=16 / FAIL=0** — 관리형 서비스 조합으로 전 기능 동작 |

배포 과정에서 관리형 서비스별로 잡은 이슈는 아래 "트러블슈팅" 참고.

## 결과 요약

- 헬스 → 회원가입/로그인(JWT) → PRO 구독 → **Spring Batch 정기결제**(`COMPLETED`) →
  결제 이력 반영 → **전체 공지(Kafka)** → 알림 수신(라운드트립) → 랭킹 재구축 전부 정상
- Kafka 토픽 8종(`notification-events` + `-retry-500/1000/2000` + `-dlt`, `subscription-events`,
  `usage-events`, `__consumer_offsets`), 컨슈머 그룹 7개 lag 0
- 가동 ~10분, **1회성 약 $0.01**, 이후 월 청구 $0

## 스크린샷

배포된 `http://<EC2_IP>` / Vercel 에서 캡처: 대시보드(백엔드 "운영 중"·룸 4개), 관리자 로그인,
관리자 페이지(룸 CRUD·공지), Kafka UI 토픽 목록. PNG 는 이 디렉터리에 추가.

## 트러블슈팅 — 상시 배포 (관리형 서비스별)

| 증상 | 원인 → 해결 |
|---|---|
| 프론트 API 전부 실패 | `SPRING_DATA_REDIS_HOST` 에 `https://` 붙은 REST URL → 호스트명만. `RedisConfig` 가 `rediss://` + host 조합 시 깨짐 |
| 백엔드 부팅 실패 (`redisson`) | Upstash 는 TLS 필수 → `SPRING_DATA_REDIS_SSL=true` + `RedisConfig` password·ssl 지원 |
| DB 연결 실패 (`Driver ... claims to not accept jdbcUrl`) | TiDB 연결문자열 `mysql://user:pass@host` 를 그대로 사용 → `jdbc:` 접두 + `user:pass@` 제거(계정은 별도 키) |
| 정기결제 500 (`isolation level 'SERIALIZABLE' is not supported`) | TiDB 는 SERIALIZABLE 미지원 → JDBC URL 에 `sessionVariables=tidb_skip_isolation_level_check=1` |
| Render health check 실패 (포트) | Render 가 `PORT`(10000) 주입 → `server.port=${PORT:8080}` |
| 콜드스타트 3~4분 | 512MB/0.1CPU — `-XX:TieredStopAtLevel=1`, Redisson/Hikari 풀 축소 (`lazy-init` 은 부작용으로 롤백) |
| 공지 후 알림 미수신 (`lazy-init` 시도 중) | `spring.main.lazy-initialization` 이 `@KafkaListener` 빈 등록 누락시킴 → 제거 |
| Kafka 컨슈머 `API_VERSIONS disconnect` 루프 | `RankingKafkaConfig`·`SubscriptionKafkaConfig` 커스텀 팩토리가 SASL 설정 누락 → `config.putAll(kafkaProperties.getProperties())` (프로듀서는 Boot 자동설정이라 정상이었음) |
