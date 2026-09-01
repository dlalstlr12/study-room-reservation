// 룸 조회 부하 — 현황판이 룸 목록과 룸별 예약현황을 반복 조회하는 상황.
//
// 캐시 전/후 비교:
//   1) 캐시 켜고 기동:  .\gradlew bootRun --args='--logging.level.root=WARN'
//   2) 캐시 끄고 기동:  .\gradlew bootRun --args='--spring.cache.type=none --logging.level.root=WARN'
//   각각:
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < backend/load-test/room-read.js
//
// 관찰 지표: http_req_duration p95, RPS(http_reqs), 실패율. 두 실행의 표를 troubleshooting.md에 기록.

import http from 'k6/http'
import { check } from 'k6'
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

http.setResponseCallback(http.expectedStatuses(200))

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
// 오늘 날짜 (현황판 기본 조회 날짜)
const TODAY = new Date().toISOString().slice(0, 10)

export const options = {
  scenarios: {
    read: { executor: 'constant-vus', vus: Number(__ENV.VUS || 30), duration: '30s' },
  },
  thresholds: {
    http_req_duration: ['p(95)<300'],
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
  },
}

export function setup() {
  const res = http.get(`${BASE_URL}/api/rooms`)
  const rooms = res.json()
  if (!rooms || rooms.length === 0) throw new Error('룸이 없습니다 — local 프로파일 시드 확인.')
  return { roomIds: rooms.map((r) => r.id) }
}

export default function (data) {
  const list = http.get(`${BASE_URL}/api/rooms`)
  check(list, { 'rooms 200': (r) => r.status === 200 })

  const roomId = data.roomIds[randomIntBetween(0, data.roomIds.length - 1)]
  const schedule = http.get(`${BASE_URL}/api/rooms/${roomId}/schedule?date=${TODAY}`)
  check(schedule, { 'schedule 200': (r) => r.status === 200 })
}
