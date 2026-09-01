import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHealth } from '../api/health'
import { listRooms } from '../api/rooms'
import { listMyReservations } from '../api/reservations'
import { useAuth } from '../auth/AuthContext'
import { Card } from '../components/ui'
import { formatDateTime } from '../utils/format'

const FEATURES = [
  { to: '/signup', label: '회원가입', desc: 'POST /api/auth/signup — 이메일 중복·검증 처리' },
  { to: '/login', label: '로그인 / 토큰 재발급', desc: 'JWT 발급, 만료 시 리프레시 토큰으로 자동 재발급' },
  { to: '/rooms', label: '룸 조회 (공개)', desc: 'GET /api/rooms — 상태 필터' },
  { to: '/rooms', label: '룸 관리 (ADMIN)', desc: 'POST/PUT/DELETE /api/rooms — 관리자 전용' },
  { to: '/reservations', label: '예약 생성·취소', desc: 'POST /api/reservations, 시간·겹침 검증' },
  { to: '/reservations', label: '내 예약 목록', desc: 'GET /api/reservations/me — 본인 예약만' },
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
          로드맵 1단계에서 구현한 백엔드 기능을 이 화면들에서 직접 확인할 수 있습니다.
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

      <Card title="1단계 기능">
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
