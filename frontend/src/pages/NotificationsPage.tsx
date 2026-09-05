import { useEffect, useState } from 'react'
import { Button, Card, EmptyState, Spinner } from '../components/ui'
import { useNotifications } from '../notifications/NotificationContext'
import { onConnectionChange } from '../realtime/stompClient'
import type { AppNotification } from '../types'

const TYPE_LABEL: Record<AppNotification['type'], string> = {
  LOTTERY_WON: '당첨',
  LOTTERY_LOST: '추첨 결과',
  ANNOUNCEMENT: '공지',
  SUBSCRIPTION_PAID: '결제',
  SUBSCRIPTION_PAYMENT_FAILED: '결제 실패',
}

export function NotificationsPage() {
  const { items, loading, markRead, markAllRead } = useNotifications()

  const [live, setLive] = useState(false)
  useEffect(() => onConnectionChange(setLive), [])

  return (
    <div className="page">
      <div className="page__head">
        <div className="page__head-row">
          <h1>알림</h1>
          <span className={`live live--${live ? 'on' : 'off'}`} title={live ? '실시간 연결됨' : '연결 끊김'}>
            실시간
          </span>
        </div>
        <p className="page__lead">
          추첨 결과와 공지가 Kafka 워커를 거쳐 여기에 쌓입니다. 발송에 실패해 DLT로 격리된 알림은{' '}
          <span className="badge badge--stop">FAILED</span>로 표시됩니다.
        </p>
      </div>

      <Card
        title="받은 알림"
        actions={
          <Button variant="ghost" onClick={markAllRead}>
            모두 읽음
          </Button>
        }
      >
        {loading && items.length === 0 && <Spinner />}
        {!loading && items.length === 0 && <EmptyState>받은 알림이 없습니다.</EmptyState>}
        {items.length > 0 && (
          <ul className="notif-list">
            {items.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className={`notif-row${n.read ? '' : ' notif-row--unread'}`}
                  onClick={() => markRead(n.id)}
                >
                  <span className="notif-row__meta">
                    <span className={`badge badge--${n.status === 'FAILED' ? 'stop' : 'done'}`}>
                      {TYPE_LABEL[n.type]}
                    </span>
                    <time className="notif-row__time">
                      {new Date(n.createdAt).toLocaleString('ko-KR', {
                        month: 'numeric',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </time>
                  </span>
                  <span className="notif-row__title">{n.title}</span>
                  <span className="notif-row__text">{n.body}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
