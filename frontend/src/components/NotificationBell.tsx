import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notifications'
import { useAuth } from '../auth/AuthContext'
import { subscribeNotifications } from '../realtime/notificationChannel'
import type { AppNotification } from '../types'
import { useToast } from './ToastContext'

const TYPE_ICON: Record<AppNotification['type'], string> = {
  LOTTERY_WON: '🎉',
  LOTTERY_LOST: '🎲',
  ANNOUNCEMENT: '📢',
}

export function NotificationBell() {
  const { user } = useAuth()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<AppNotification[]>([])
  const [unread, setUnread] = useState(0)

  // 로그인 상태가 되면(또는 계정이 바뀌면) 최초 로드
  useEffect(() => {
    if (!user) {
      setItems([])
      setUnread(0)
      return
    }
    let active = true
    Promise.all([listNotifications(), getUnreadCount()])
      .then(([list, count]) => {
        if (!active) return
        setItems(list)
        setUnread(count.count)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [user])

  // 실시간 수신 (ref 패턴으로 effect는 memberId에만 의존)
  const onIncoming = useRef<(n: AppNotification) => void>(() => {})
  onIncoming.current = (n) => {
    setItems((prev) => [n, ...prev.filter((x) => x.id !== n.id)].slice(0, 20))
    setUnread((c) => c + 1)
    if (n.type === 'LOTTERY_WON') toast.success(`${n.title} — ${n.body}`)
    else toast.info(n.title)
  }
  useEffect(() => {
    if (!user) return
    return subscribeNotifications(user.id, (n) => onIncoming.current(n))
  }, [user])

  const handleOpen = () => setOpen((v) => !v)

  const handleRead = async (n: AppNotification) => {
    if (n.read) return
    setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)))
    setUnread((c) => Math.max(0, c - 1))
    try {
      await markNotificationRead(n.id)
    } catch {
      /* 낙관적 업데이트 유지 */
    }
  }

  const handleReadAll = async () => {
    setItems((prev) => prev.map((x) => ({ ...x, read: true })))
    setUnread(0)
    try {
      await markAllNotificationsRead()
    } catch {
      /* noop */
    }
  }

  if (!user) return null

  return (
    <div className="bell">
      <button
        type="button"
        className="bell__btn"
        aria-label={`알림 ${unread}건`}
        aria-expanded={open}
        onClick={handleOpen}
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
                  <button type="button" className="bell__link" onClick={handleReadAll}>
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
                      onClick={() => handleRead(n)}
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
