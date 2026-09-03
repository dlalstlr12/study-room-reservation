// 알림 팬아웃 부하 — ADMIN 전체 공지를 반복 발행해 워커 처리량·lag 를 관찰한다.
//
// 기동:
//   .\gradlew bootRun --args='--logging.level.root=WARN'
//   발송 장애를 섞으려면:  --args='--notification.delivery.failure-rate=0.3 ...'
// 실행:
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < backend/load-test/notification-announce.js
//
// 관찰:
//   - 엔드포인트 응답(http_req_duration): 발행은 fire-and-forget 이라 회원 수와 무관하게 낮아야 함
//   - kafka-ui(localhost:8085): notification-events 소비 lag, -retry-*/-dlt 유입
//   - 앱 로그: [알림 발송] 처리량, [알림 DLT] 최종 실패 건수
//   - DB: select status, count(*) from notifications group by status

import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@studyroom.local'
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin1234'

export const options = {
  scenarios: {
    announce: { executor: 'constant-arrival-rate', rate: Number(__ENV.RATE || 2), timeUnit: '1s',
      duration: '30s', preAllocatedVUs: 5 },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:announce}': ['p(95)<500'],
  },
}

export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: ADMIN_EMAIL, password: ADMIN_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } })
  check(res, { 'login 200': (r) => r.status === 200 })
  return { token: res.json('accessToken') }
}

export default function (data) {
  const res = http.post(`${BASE_URL}/api/notifications/announcements`, JSON.stringify({
    title: `부하 공지 ${Date.now()}`,
    body: '워커 처리량 측정용 공지입니다.',
  }), {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` },
    tags: { name: 'announce' },
  })
  check(res, { '202 accepted': (r) => r.status === 202 })
}
