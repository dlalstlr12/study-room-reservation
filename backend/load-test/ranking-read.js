// 랭킹 조회 부하 — 랭킹판은 Redis Sorted Set 직결이라 이력이 쌓여도 O(log N).
//
// 기동:
//   .\gradlew bootRun --args='--logging.level.root=WARN'
// 실행:
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < backend/load-test/ranking-read.js
//
// 관찰: http_req_duration p95, RPS. /me 는 순수 Redis(ZREVRANK+ZSCORE), 목록은 이름 IN 조회가 붙는다.
// 비교용: usage_logs 를 크게 적재한 뒤 DB GROUP BY 집계 쿼리의 응답과 대조하면 어필 포인트가 된다.

import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@studyroom.local'
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin1234'

http.setResponseCallback(http.expectedStatuses(200))

export const options = {
  scenarios: {
    read: { executor: 'constant-vus', vus: Number(__ENV.VUS || 30), duration: '30s' },
  },
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
}

export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: ADMIN_EMAIL, password: ADMIN_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } })
  return { token: res.json('accessToken') }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }
  const list = http.get(`${BASE_URL}/api/rankings?scope=all&limit=20`, { headers, tags: { name: 'top' } })
  const me = http.get(`${BASE_URL}/api/rankings/me?scope=all`, { headers, tags: { name: 'me' } })
  check(list, { 'top 200': (r) => r.status === 200 })
  check(me, { 'me 200': (r) => r.status === 200 })
}
