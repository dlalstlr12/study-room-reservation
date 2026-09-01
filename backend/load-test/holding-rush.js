// 홀딩 러시 — 여러 사용자가 "같은 룸·같은 슬롯"을 동시에 홀딩하려 한다.
//
// 실행 (백엔드가 8080에서 떠 있어야 함):
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < backend/load-test/holding-rush.js
//
// 관찰 지표: held_201(홀딩 성공 카운트, 이상적으로 1), 409 비율, p95, RPS.
// 2단계 예약 러시와 비교: 홀딩은 "확정 단계 몰림"을 앞단에서 빠른 실패로 바꾼다.

import http from 'k6/http'
import { check } from 'k6'
import { Counter } from 'k6/metrics'
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ROOM_ID = Number(__ENV.ROOM_ID || 2)
const MEMBERS = Number(__ENV.MEMBERS || 20)

// 모든 반복이 노리는 단 하나의 30분 정렬 슬롯 (미래 고정)
const SLOT = { startAt: '2027-03-02T10:00:00', endAt: '2027-03-02T11:00:00' }

const held201 = new Counter('held_201')

// 201·409(정상 홀딩/충돌), 200(setup 로그인)만 정상 취급
http.setResponseCallback(http.expectedStatuses(200, 201, 409))

export const options = {
  scenarios: {
    rush: { executor: 'constant-vus', vus: Number(__ENV.VUS || 20), duration: '30s' },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.02'],
  },
}

const JSON_HEADERS = { 'Content-Type': 'application/json' }

export function setup() {
  const stamp = Date.now()
  const tokens = []
  for (let i = 0; i < MEMBERS; i++) {
    const email = `hold-${stamp}-${i}@test.local`
    http.post(`${BASE_URL}/api/auth/signup`,
      JSON.stringify({ email, password: 'password1', name: `hold${i}` }), { headers: JSON_HEADERS })
    const login = http.post(`${BASE_URL}/api/auth/login`,
      JSON.stringify({ email, password: 'password1' }), { headers: JSON_HEADERS })
    if (login.status === 200) tokens.push(login.json('accessToken'))
  }
  if (tokens.length === 0) throw new Error('로그인 토큰 확보 실패 — 백엔드 상태 확인.')
  return { tokens }
}

export default function (data) {
  const token = data.tokens[randomIntBetween(0, data.tokens.length - 1)]
  const res = http.post(
    `${BASE_URL}/api/reservations/holds`,
    JSON.stringify({ roomId: ROOM_ID, startAt: SLOT.startAt, endAt: SLOT.endAt }),
    { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } },
  )

  check(res, { 'status is 201 or 409': (r) => r.status === 201 || r.status === 409 })
  if (res.status === 201) held201.add(1)
}
