# deploy/

로드맵 9단계 배포 자산. 두 갈래로 나뉜다.

| 목적 | 방식 | 비용 | 파일 |
|---|---|---|---|
| **AWS 경험 확보** (1회성) | EC2 1대에 전체 스택 자체 호스팅 → 동작 확인 → 캡처 → **Terminate** | 검증 몇 시간치 (~$1 미만), 이후 0 | `docker-compose.prod.yml`, `ec2-user-data.sh`, `deploy.sh`, `CHECKLIST.md` |
| **상시 데모** | 프론트 Vercel · 백엔드 Render · MySQL TiDB Cloud · Redis Upstash · Kafka Confluent Cloud Basic | ~$0 | `../render.yaml`, `../frontend/vercel.json`, `RENDER.md` |

## 구성요소

```
backend/Dockerfile        멀티스테이지 bootJar → JRE. SPRING_PROFILES_ACTIVE=demo 기본.
frontend/Dockerfile       Vite 빌드 → nginx. /api·/ws 를 backend:8080 으로 프록시.
frontend/nginx.conf       SPA 폴백 + 리버스 프록시 (같은 오리진 → CORS 불필요).
backend/.../application-demo.yml   전 항목 환경변수 주입. 기본값은 compose 서비스명 기준.

deploy/docker-compose.prod.yml   mysql·redis·kafka(KRaft)·backend·frontend·kafka-ui
deploy/.env.prod.example         → deploy/.env 로 복사 (gitignore). 시크릿.
deploy/ec2-user-data.sh          cloud-init: 도커 설치 + swap 2G + git clone
deploy/deploy.sh                 EC2 안에서 실행: IP 자동 감지 → .env → up --build → 헬스 대기
deploy/verify.sh                 엔드투엔드 스모크 테스트 → verify-*.log
deploy/CHECKLIST.md              AWS CLI 단계별 + 캡처 지점 + 정리(Terminate) 절차
```

## 빠른 로컬 검증 (AWS 비용 0)

```bash
cp deploy/.env.prod.example deploy/.env
#  JWT_SECRET 32자 이상,  APP_CORS_ALLOWED_ORIGINS=http://localhost 로 편집
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build
BASE=http://localhost bash deploy/verify.sh
docker compose -f deploy/docker-compose.prod.yml down -v
```

## 환경변수 (application-demo.yml)

| 변수 | 용도 | EC2 기본값 |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | MySQL(또는 TiDB) | `jdbc:mysql://mysql:3306/...` |
| `SPRING_DATA_REDIS_HOST` / `_PORT` | Redis 호스트·포트 | `redis` / `6379` |
| `SPRING_DATA_REDIS_PASSWORD` / `_SSL` | Upstash 등 인증·TLS 시에만 | (빈값 / `false`) |
| `KAFKA_BOOTSTRAP_SERVERS` | 브로커 | `kafka:29092` |
| `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL` 등 | Upstash SASL_SSL 시에만 | (미설정 = PLAINTEXT) |
| `JWT_SECRET` | HMAC 키 32B+ | (필수) |
| `APP_CORS_ALLOWED_ORIGINS` | 프론트 오리진 | `http://<EC2_IP>` (deploy.sh 자동) |

상시 데모(Render 등)에서는 위 값들을 관리형 서비스 접속정보로 덮어쓴다 — `CHECKLIST.md` 참고.
