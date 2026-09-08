import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useBackendStatus } from '../backend/BackendStatusContext'
import { listRooms } from '../api/rooms'
import { listMyReservations } from '../api/reservations'
import { useAuth } from '../auth/AuthContext'
import { Card } from '../components/ui'

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
  const { state: backend, waitingMs, readyCount } = useBackendStatus()
  const [stats, setStats] = useState<{ rooms?: number; reservations?: number }>({})

  // 서버가 잠들어 있었다면 첫 조회는 실패한다. 살아난 순간(readyCount)에 다시 불러온다.
  useEffect(() => {
    if (backend !== 'up') return
    listRooms()
      .then((rooms) => setStats((s) => ({ ...s, rooms: rooms.length })))
      .catch(() => undefined)
    if (status === 'authenticated') {
      listMyReservations()
        .then((r) => setStats((s) => ({ ...s, reservations: r.length })))
        .catch(() => undefined)
    }
  }, [status, backend, readyCount])

  const waking = backend !== 'up' && waitingMs >= 6_000

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
          <p className={`stat stat--${backend === 'up' ? 'up' : waking ? 'waking' : 'checking'}`}>
            {backend === 'up' ? '운영 중' : waking ? '기동 중' : '확인 중'}
          </p>
          {waking && <p className="stat__sub">무료 티어라 첫 접속에 3~5분이 걸립니다</p>}
        </Card>
        <Link to="/rooms" className="stat-card-link">
          <Card title="등록된 룸">
            <p className="stat">{stats.rooms ?? '—'}<span className="stat__unit">개</span></p>
          </Card>
        </Link>
        <Link to="/reservations" className="stat-card-link">
          <Card title="내 예약">
            <p className="stat">
              {status === 'authenticated' ? stats.reservations ?? '—' : '—'}
              <span className="stat__unit">건</span>
            </p>
            {status !== 'authenticated' && <p className="stat__sub">로그인하면 표시됩니다</p>}
          </Card>
        </Link>
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
