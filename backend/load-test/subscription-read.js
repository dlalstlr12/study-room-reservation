// 구독 조회 부하 — 플랜 카드와 결제 이력을 반복 조회하는 상황.
//
// 기동:
//   .\gradlew bootRun --args='--logging.level.root=WARN'
// 실행:
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < backend/load-test/subscription-read.js
//
// 배치 처리량은 BillingJobTest 로그와 POST /api/admin/billing/run 응답으로 측정한다.

import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const EMAIL = __ENV.EMAIL || 'admin@studyroom.local'
const PASSWORD = __ENV.PASSWORD || 'admin1234'

http.setResponseCallback(http.expectedStatuses(200))

export const options = {
  scenarios: {
    read: { executor: 'constant-vus', vus: Number(__ENV.VUS || 20), duration: '30s' },
  },
  thresholds: {
    http_req_duration: ['p(95)<150'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
}

export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email: EMAIL, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
  })
  return { token: res.json('accessToken') }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }
  const me = http.get(`${BASE_URL}/api/subscriptions/me`, { headers, tags: { name: 'me' } })
  const payments = http.get(`${BASE_URL}/api/subscriptions/me/payments`, { headers, tags: { name: 'payments' } })
  check(me, { 'me 200': (r) => r.status === 200 })
  check(payments, { 'payments 200': (r) => r.status === 200 })
}
