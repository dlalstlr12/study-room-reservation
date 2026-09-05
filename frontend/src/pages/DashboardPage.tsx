import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHealth } from '../api/health'
import { listRooms } from '../api/rooms'
import { listMyReservations } from '../api/reservations'
import { useAuth } from '../auth/AuthContext'
import { Card } from '../components/ui'
import { formatDateTime } from '../utils/format'

const FEATURES = [
  { to: '/rooms', label: '룸', desc: '룸별 실시간 좌석 현황을 보고 원하는 시간대를 홀딩·예약합니다' },
  { to: '/reservations', label: '내 예약', desc: '내가 홀딩·예약한 내역을 확인하고 취소하거나 이용을 마치면 퇴실 처리합니다' },
  { to: '/lottery', label: '이벤트 추첨', desc: '이용 중인 회원 또는 전체 회원을 대상으로 추첨하고, 결과를 실시간으로 발표합니다' },
  { to: '/notifications', label: '알림', desc: '추첨 결과, 결제 내역 등 나에게 온 소식을 실시간으로 확인합니다' },
  { to: '/ranking', label: '랭킹', desc: '누적 이용 시간을 기준으로 순위를 매겨 보여줍니다' },
  { to: '/subscription', label: '구독', desc: 'PRO를 구독하면 좌석 홀딩 유예 시간이 늘어나고, 정기결제 내역을 확인할 수 있습니다' },
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
          스터디룸을 편하게 예약하고 이용할 수 있도록 돕는 스터디룸 예약 시스템입니다.
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

      <Card title="이용 가능한 기능">
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
