import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useNotifications } from '../notifications/NotificationContext'
import type { AppNotification } from '../types'

const TYPE_ICON: Record<AppNotification['type'], string> = {
  LOTTERY_WON: '🎉',
  LOTTERY_LOST: '🎲',
  ANNOUNCEMENT: '📢',
}

export function NotificationBell() {
  const { user } = useAuth()
  const { items, unread, markRead, markAllRead } = useNotifications()
  const [open, setOpen] = useState(false)

  if (!user) return null

  return (
    <div className="bell">
      <button
        type="button"
        className="bell__btn"
        aria-label={`알림 ${unread}건`}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.4" aria-hidden="true">
          <path d="M8 2a3.5 3.5 0 0 0-3.5 3.5c0 3-1.5 4-1.5 4h10s-1.5-1-1.5-4A3.5 3.5 0 0 0 8 2Z" />
          <path d="M6.5 12.5a1.5 1.5 0 0 0 3 0" />
        </svg>
        {unread > 0 && <span className="bell__count">{unread > 99 ? '99+' : unread}</span>}
      </button>

      {open && (
        <>
          <div className="bell__scrim" onClick={() => setOpen(false)} />
          <div className="bell__panel" role="menu">
            <header className="bell__panel-head">
              <strong>알림</strong>
              <div className="bell__panel-actions">
                {unread > 0 && (
                  <button type="button" className="bell__link" onClick={markAllRead}>
                    모두 읽음
                  </button>
                )}
                <Link to="/notifications" className="bell__link" onClick={() => setOpen(false)}>
                  전체 보기
                </Link>
              </div>
            </header>
            {items.length === 0 ? (
              <p className="bell__empty">받은 알림이 없습니다.</p>
            ) : (
              <ul className="bell__list">
                {items.slice(0, 10).map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      className={`notif${n.read ? '' : ' notif--unread'}`}
                      onClick={() => markRead(n.id)}
                    >
                      <span className="notif__icon" aria-hidden="true">
                        {TYPE_ICON[n.type]}
                      </span>
                      <span className="notif__body">
                        <span className="notif__title">{n.title}</span>
                        <span className="notif__text">{n.body}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  )
}
