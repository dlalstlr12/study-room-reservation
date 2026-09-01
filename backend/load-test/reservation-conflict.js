// 예약 동시성 부하테스트 — 여러 사용자가 "같은 룸·같은 시간"으로 동시에 예약을 시도한다.
//
// 실행 (백엔드가 8080에서 떠 있어야 함):
//   docker run --rm -i --network host grafana/k6 run - < backend/load-test/reservation-conflict.js
//
// 락 전략을 바꿔 재기동한 뒤 3회 실행해 비교한다:
//   application.yml 의 reservation.lock.strategy = none | pessimistic | distributed
//
// 관찰 지표: created_201(성공 카운트, 이상적으로 1), 409 비율, http_req_duration p95, RPS.

import http from 'k6/http'
import { check } from 'k6'
import { Counter } from 'k6/metrics'
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ROOM_ID = Number(__ENV.ROOM_ID || 2)
const MEMBERS = Number(__ENV.MEMBERS || 20)

// 모든 반복이 노리는 단 하나의 시간대 (미래 고정)
const SLOT = { startAt: '2027-01-05T10:00:00', endAt: '2027-01-05T11:00:00' }

const created201 = new Counter('created_201')

// 201·409 는 정상 응답으로 취급 (409 = 이미 예약됨 / 락 타임아웃)
http.setResponseCallback(http.expectedStatuses(201, 409))

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
    const email = `load-${stamp}-${i}@test.local`
    const body = JSON.stringify({ email, password: 'password1', name: `load${i}` })
    http.post(`${BASE_URL}/api/auth/signup`, body, { headers: JSON_HEADERS }) // 201 또는 409(재실행)
    const login = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email, password: 'password1' }),
      { headers: JSON_HEADERS },
    )
    if (login.status === 200) tokens.push(login.json('accessToken'))
  }
  if (tokens.length === 0) throw new Error('로그인 토큰을 하나도 확보하지 못했습니다. 백엔드 상태 확인.')
  return { tokens }
}

export default function (data) {
  const token = data.tokens[randomIntBetween(0, data.tokens.length - 1)]
  const res = http.post(
    `${BASE_URL}/api/reservations`,
    JSON.stringify({ roomId: ROOM_ID, startAt: SLOT.startAt, endAt: SLOT.endAt }),
    { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } },
  )

  // 정상 결과는 201(예약 성공) 또는 409(이미 예약됨 / 락 타임아웃) 뿐이다.
  check(res, { 'status is 201 or 409': (r) => r.status === 201 || r.status === 409 })
  if (res.status === 201) created201.add(1)
}
