import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { getHealth } from '../api/health'
import { useAuth } from '../auth/AuthContext'
import { NotificationBell } from './NotificationBell'
import { useToast } from './ToastContext'
import { Button } from './ui'

type HealthState = 'checking' | 'up' | 'down'

const NAV = [
  { to: '/', label: '대시보드', end: true },
  { to: '/rooms', label: '룸', end: false },
  { to: '/reservations', label: '내 예약', end: false },
  { to: '/lottery', label: '이벤트 추첨', end: false },
  { to: '/notifications', label: '알림', end: false },
  { to: '/ranking', label: '랭킹', end: false },
]

export function AppLayout() {
  const { user, status, logout } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [health, setHealth] = useState<HealthState>('checking')

  useEffect(() => {
    let active = true
    const check = () =>
      getHealth()
        .then(() => active && setHealth('up'))
        .catch(() => active && setHealth('down'))
    check()
    const timer = window.setInterval(check, 15000)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [])

  const handleLogout = async () => {
    await logout()
    toast.info('로그아웃되었습니다.')
    navigate('/')
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <span className="sidebar__logo" aria-hidden="true">
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6">
              <rect x="2" y="2" width="12" height="12" rx="1" />
              <path d="M9.5 2v12" />
              <circle cx="7.7" cy="8" r="0.6" fill="currentColor" stroke="none" />
            </svg>
          </span>
          <div>
            <strong>스터디룸 예약</strong>
            <span className="sidebar__sub">STEP 7 · RANKING</span>
          </div>
        </div>
        <nav className="sidebar__nav">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `sidebar__link${isActive ? ' is-active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__foot">
          <span className={`health health--${health}`}>
            <span className="health__dot" />
            {health === 'up' ? '운영 중' : health === 'down' ? '응답 없음' : '확인 중'}
          </span>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <NotificationBell />
          <div className="topbar__account">
            {status === 'authenticated' && user ? (
              <>
                <span className="topbar__email">{user.email}</span>
                <span className={`badge ${user.role === 'ADMIN' ? 'badge--admin' : 'badge--done'}`}>
                  {user.role}
                </span>
                <Button variant="ghost" onClick={handleLogout}>
                  로그아웃
                </Button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="btn btn--ghost">
                  로그인
                </NavLink>
                <NavLink to="/signup" className="btn btn--primary">
                  회원가입
                </NavLink>
              </>
            )}
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
