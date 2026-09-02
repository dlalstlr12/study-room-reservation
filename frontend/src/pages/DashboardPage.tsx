import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHealth } from '../api/health'
import { listRooms } from '../api/rooms'
import { listMyReservations } from '../api/reservations'
import { useAuth } from '../auth/AuthContext'
import { Card } from '../components/ui'
import { formatDateTime } from '../utils/format'

const FEATURES = [
  { to: '/rooms', label: '실시간 좌석 현황', desc: 'WebSocket /topic/rooms/{id} — 다른 사용자의 홀딩·예약이 새로고침 없이 반영' },
  { to: '/rooms', label: '룸 예약 현황', desc: 'GET /api/rooms/{id}/schedule — 예약·홀딩 타임라인 (Redis 캐싱)' },
  { to: '/rooms', label: '좌석 홀딩', desc: 'POST /api/reservations/holds — Redis TTL 10분, 30분 슬롯' },
  { to: '/reservations', label: '홀딩 확정 / 내 예약', desc: 'POST /holds/.../confirm, GET /holds/me — TTL 만료 시 자동 해제' },
]

export function DashboardPage() {
  const { user, status } = useAuth()
  const [health, setHealth] = useState<{ state: 'up' | 'down' | 'checking'; timestamp?: string }>({
    state: 'checking',
  })
  const [stats, setStats] = useState<{ rooms?: number; reservations?: number }>({})

  useEffect(() => {
    getHealth()
      .then((h) => setHealth({ state: 'up', timestamp: h.timestamp }))
      .catch(() => setHealth({ state: 'down' }))
  }, [])

  useEffect(() => {
    listRooms()
      .then((rooms) => setStats((s) => ({ ...s, rooms: rooms.length })))
      .catch(() => undefined)
    if (status === 'authenticated') {
      listMyReservations()
        .then((r) => setStats((s) => ({ ...s, reservations: r.length })))
        .catch(() => undefined)
    }
  }, [status])

  return (
    <div className="page">
      <div className="page__head">
        <h1>대시보드</h1>
        <p className="page__lead">
          로드맵 4단계 — WebSocket으로 좌석 현황을 실시간 브로드캐스트. 아래 화면들에서 직접 확인할 수 있습니다.
        </p>
      </div>

      <div className="grid grid--stats">
        <Card title="백엔드 상태">
          <p className={`stat stat--${health.state}`}>
            {health.state === 'up' && '운영 중'}
            {health.state === 'down' && '응답 없음'}
            {health.state === 'checking' && '확인 중'}
          </p>
          {health.state === 'down' && (
            <p className="stat__sub">백엔드를 실행한 뒤 새로고침하세요.</p>
          )}
          {health.timestamp && (
            <p className="stat__sub">서버 시각 {formatDateTime(health.timestamp)}</p>
          )}
        </Card>
        <Card title="등록된 룸">
          <p className="stat">{stats.rooms ?? '—'}<span className="stat__unit">개</span></p>
        </Card>
        <Card title="내 예약">
          <p className="stat">
            {status === 'authenticated' ? stats.reservations ?? '—' : '—'}
            <span className="stat__unit">건</span>
          </p>
          {status !== 'authenticated' && <p className="stat__sub">로그인하면 표시됩니다</p>}
        </Card>
      </div>

      <Card title="4단계 기능">
        <ul className="feature-list">
          {FEATURES.map((f) => (
            <li key={f.label}>
              <Link to={f.to} className="feature-list__link">
                {f.label}
              </Link>
              <span className="feature-list__desc">{f.desc}</span>
            </li>
          ))}
        </ul>
      </Card>

      {status === 'authenticated' && user && (
        <Card title="현재 로그인">
          <p>
            <strong>{user.name}</strong> ({user.email}) · 권한 {user.role}
          </p>
        </Card>
      )}
    </div>
  )
}
