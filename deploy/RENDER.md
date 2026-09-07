# 상시 무료 배포 — Render + Vercel + TiDB + Upstash + Confluent

AWS EC2 검증(1회성)과 별개로, 항상 켜져 있는 데모 링크를 무료로 유지한다.

```
프론트  Vercel                        https://<app>.vercel.app
백엔드  Render (Docker, free web)     https://<svc>.onrender.com
MySQL   TiDB Cloud Serverless (무료)
Redis   Upstash Redis (무료)
Kafka   Confluent Cloud Basic         클러스터 요금 없음, 데모 트래픽 ~$0
```

> Render 무료 웹서비스는 15분 무접속 시 잠들고 첫 요청에 30~50초 콜드스타트.
> README 에 "첫 로딩은 서버가 깨어나는 중" 문구를 넣어둔다.

---

## 1. TiDB Cloud Serverless (MySQL)

1. https://tidbcloud.com → Serverless 클러스터 생성 (리전: `ap-northeast-1` 등)
2. **Connect** → `Connection String` / `Parameters` 에서 host·port(4000)·user·password 확보
3. SQL 콘솔 또는 클라이언트로 DB 생성: `CREATE DATABASE study_room;`
4. Render 환경변수:

| key | value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<host>:4000/study_room?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3&serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `SPRING_DATASOURCE_USERNAME` | `<tidb user>` (형식: `xxxxxxxx.root`) |
| `SPRING_DATASOURCE_PASSWORD` | `<password>` |

> Flyway 가 부팅 시 `V1~V8` 마이그레이션을 자동 적용한다. 시드 데이터(데모 관리자·룸)는
> `demo` 프로파일이 생성.

---

## 2. Upstash Redis

1. https://console.upstash.com → Redis DB 생성 (Global 또는 리전 선택)
2. **Details** 에서 `Endpoint`(host), `Port`, `Password` 확보 (TLS 필수)
3. Render 환경변수:

| key | value |
|---|---|
| `SPRING_DATA_REDIS_HOST` | `<endpoint>` |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | `<password>` |
| `SPRING_DATA_REDIS_SSL` | `true` |

---

## 3. Confluent Cloud Basic (Kafka)

1. https://confluent.cloud → **Basic** 클러스터 생성 (클라우드/리전: AWS `ap-northeast-2` 권장)
2. **API Keys** → 클러스터용 키 생성 → `Key` / `Secret` 확보
3. **Cluster settings** → `Bootstrap server` 확보 (`pkc-xxxxx.<region>.aws.confluent.cloud:9092`)
4. **토픽 수동 생성** (Confluent 는 클라이언트 자동생성 비활성) — 각 파티션 1, 복제 3(기본):

   ```
   notification-events
   notification-events-retry-500
   notification-events-retry-1000
   notification-events-retry-2000
   notification-events-dlt
   subscription-events
   usage-events
   ```

5. Render 환경변수:

| key | value |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `pkc-xxxxx.<region>.aws.confluent.cloud:9092` |
| `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL` | `SASL_SSL` |
| `SPRING_KAFKA_PROPERTIES_SASL_MECHANISM` | `PLAIN` |
| `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` | `org.apache.kafka.common.security.plain.PlainLoginModule required username="<API_KEY>" password="<API_SECRET>";` |

> 비용: Basic 은 클러스터 시간요금이 없고 ingress/egress/스토리지만 과금. 이 데모 트래픽
> (하루 메시지 몇 건)이면 월 수백 원 이하이며 가입 크레딧으로 초반 상쇄된다.

---

## 4. Render (백엔드)

`render.yaml` Blueprint 사용:

1. https://dashboard.render.com → **New > Blueprint** → 이 저장소 연결
2. Blueprint 가 `study-room-backend` 웹서비스를 생성 — `sync:false` 환경변수를 위 1~3에서 확보한 값으로 채운다
3. `APP_CORS_ALLOWED_ORIGINS` 는 5단계에서 Vercel 도메인 확정 후 입력 (`https://<app>.vercel.app`)
4. Deploy → `https://<svc>.onrender.com/api/health` 가 `{"status":"UP"}` 확인
5. `autoDeploy: true` — 이후 `main` push 시 자동 재배포 (CI 통과와 무관하게 Render 가 독립 빌드하므로,
   CI 실패 시 배포를 막고 싶으면 Render Deploy Hook + GitHub Actions 연동으로 전환)

---

## 5. Vercel (프론트)

1. https://vercel.com → **Add New > Project** → 이 저장소 import
2. **Root Directory**: `frontend`  (모노레포)
3. Framework: Vite (자동 감지, `frontend/vercel.json` 이 SPA 리라이트 처리)
4. 환경변수:

| key | value |
|---|---|
| `VITE_API_BASE_URL` | `https://<svc>.onrender.com` |

5. Deploy → 배포 도메인(`https://<app>.vercel.app`) 확인
6. **되돌아가서** Render 의 `APP_CORS_ALLOWED_ORIGINS` 에 이 도메인 입력 → Render 재배포

---

## 6. 연결 검증

```bash
BASE=https://<svc>.onrender.com bash deploy/verify.sh
```

- [ ] `verify.sh` PASS (콜드스타트 때문에 첫 실행은 타임아웃 가능 — 재실행)
- [ ] Vercel 앱에서 로그인(`admin@studyroom.local` / `admin1234`) → 관리자 페이지
- [ ] 관리자: 정기결제 실행 → 알림 벨에 실시간 알림 (Confluent 라운드트립)
- [ ] 랭킹/구독/공지 화면 정상
- [ ] 브라우저 콘솔에 CORS·WebSocket 에러 없음

---

## 배포 흐름 (CD)

```
main push
  ├─ GitHub Actions CI (빌드·테스트)         — 게이트/검증
  ├─ Render  : 저장소 감지 → Docker 빌드 → 배포  (autoDeploy)
  └─ Vercel  : 저장소 감지 → Vite 빌드 → 배포    (자동)
```

관리형 플랫폼의 Git 연동이 CD 역할을 하므로 별도 배포 스크립트가 없다.
