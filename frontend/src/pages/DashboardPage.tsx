import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHealth } from '../api/health'
import { listRooms } from '../api/rooms'
import { listMyReservations } from '../api/reservations'
import { useAuth } from '../auth/AuthContext'
import { Card } from '../components/ui'
import { formatDateTime } from '../utils/format'

const FEATURES = [
  { to: '/lottery', label: '이벤트 추첨', desc: '기준 시각 이용자 스냅샷 → 시드 기반 재현 가능한 추첨, 스케줄러 자동 실행' },
  { to: '/lottery', label: '당첨 실시간 발표', desc: 'WebSocket /topic/lottery — 추첨 커밋 후 새로고침 없이 결과' },
  { to: '/rooms', label: '실시간 좌석 현황 · 홀딩', desc: 'WebSocket /topic/rooms/{id}, Redis TTL 홀딩, 룸 현황 캐싱' },
  { to: '/reservations', label: '내 홀딩 / 예약', desc: 'GET /holds/me, /reservations/me' },
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
          로드맵 5단계 — 이용 중이던 회원 대상 이벤트 추첨. 아래 화면들에서 직접 확인할 수 있습니다.
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

      <Card title="5단계 기능">
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
