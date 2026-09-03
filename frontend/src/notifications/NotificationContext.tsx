import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notifications'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { subscribeNotifications } from '../realtime/notificationChannel'
import type { AppNotification } from '../types'

interface NotificationContextValue {
  items: AppNotification[]
  unread: number
  loading: boolean
  markRead: (id: number) => void
  markAllRead: () => void
  reload: () => void
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

/**
 * 알림 상태를 한 곳에서 관리한다 — 벨(topbar)과 알림 페이지가 같은 목록·안읽음 수를 공유하므로,
 * 한쪽에서 읽으면 다른 쪽도 즉시 반영된다.
 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const toast = useToast()
  const [items, setItems] = useState<AppNotification[]>([])
  const [unread, setUnread] = useState(0)
  const [loading, setLoading] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    return Promise.all([listNotifications(), getUnreadCount()])
      .then(([list, count]) => {
        setItems(list)
        setUnread(count.count)
      })
      .catch(() => undefined)
      .finally(() => setLoading(false))
  }, [])

  // 로그인 상태가 되면(또는 계정이 바뀌면) 로드, 로그아웃하면 비운다
  useEffect(() => {
    if (!user) {
      setItems([])
      setUnread(0)
      return
    }
    void load()
  }, [user, load])

  // 실시간 수신 — effect는 memberId에만 의존 (ref 패턴)
  const onIncoming = useRef<(n: AppNotification) => void>(() => {})
  onIncoming.current = (n) => {
    setItems((prev) => [n, ...prev.filter((x) => x.id !== n.id)].slice(0, 50))
    setUnread((c) => c + 1)
    if (n.type === 'LOTTERY_WON') toast.success(`${n.title} — ${n.body}`)
    else toast.info(n.title)
  }
  useEffect(() => {
    if (!user) return
    return subscribeNotifications(user.id, (n) => onIncoming.current(n))
  }, [user])

  const markRead = useCallback((id: number) => {
    setItems((prev) => {
      const target = prev.find((x) => x.id === id)
      if (!target || target.read) return prev
      setUnread((c) => Math.max(0, c - 1))
      return prev.map((x) => (x.id === id ? { ...x, read: true } : x))
    })
    markNotificationRead(id).catch(() => undefined)
  }, [])

  const markAllRead = useCallback(() => {
    setItems((prev) => prev.map((x) => ({ ...x, read: true })))
    setUnread(0)
    markAllNotificationsRead().catch(() => undefined)
  }, [])

  const value: NotificationContextValue = {
    items,
    unread,
    loading,
    markRead,
    markAllRead,
    reload: load,
  }

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}

export function useNotifications(): NotificationContextValue {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotifications must be used within NotificationProvider')
  return ctx
}
