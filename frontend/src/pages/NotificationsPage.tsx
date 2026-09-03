import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  sendAnnouncement,
} from '../api/notifications'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Input, Spinner, Textarea } from '../components/ui'
import { useApi } from '../hooks/useApi'
import { onConnectionChange } from '../realtime/stompClient'
import { subscribeNotifications } from '../realtime/notificationChannel'
import type { AppNotification } from '../types'

const TYPE_LABEL: Record<AppNotification['type'], string> = {
  LOTTERY_WON: '당첨',
  LOTTERY_LOST: '추첨 결과',
  ANNOUNCEMENT: '공지',
}

export function NotificationsPage() {
  const { user, isAdmin } = useAuth()
  const feed = useApi<AppNotification[]>(() => listNotifications(), [])

  const [live, setLive] = useState(false)
  useEffect(() => onConnectionChange(setLive), [])

  const reload = useRef(feed.reload)
  reload.current = feed.reload
  useEffect(() => {
    if (!user) return
    return subscribeNotifications(user.id, () => reload.current())
  }, [user])

  const handleRead = async (n: AppNotification) => {
    if (n.read) return
    try {
      await markNotificationRead(n.id)
    } finally {
      feed.reload()
    }
  }

  const handleReadAll = async () => {
    await markAllNotificationsRead()
    feed.reload()
  }

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

      {isAdmin && <AnnouncementForm />}

      <Card
        title="받은 알림"
        actions={
          <Button variant="ghost" onClick={handleReadAll}>
            모두 읽음
          </Button>
        }
      >
        {feed.loading && <Spinner />}
        {feed.error && <EmptyState>{feed.error}</EmptyState>}
        {feed.data && feed.data.length === 0 && <EmptyState>받은 알림이 없습니다.</EmptyState>}
        {feed.data && feed.data.length > 0 && (
          <ul className="notif-list">
            {feed.data.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className={`notif-row${n.read ? '' : ' notif-row--unread'}`}
                  onClick={() => handleRead(n)}
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

function AnnouncementForm() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [open, setOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    if (!title.trim() || !body.trim()) {
      setError('제목과 내용을 입력하세요.')
      return
    }
    setSubmitting(true)
    try {
      await sendAnnouncement({ title: title.trim(), body: body.trim() })
      toast.success('전체 공지를 발송했습니다. 워커가 순차 처리합니다.')
      setTitle('')
      setBody('')
      setOpen(false)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '발송에 실패했습니다.'
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card
      title="전체 공지 보내기"
      actions={
        <Button variant={open ? 'ghost' : 'secondary'} onClick={() => setOpen((v) => !v)}>
          {open ? '닫기' : '새 공지'}
        </Button>
      }
    >
      {open ? (
        <form className="form" onSubmit={submit} noValidate>
          <Field label="제목">
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="서비스 점검 안내" />
          </Field>
          <Field label="내용">
            <Textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={3}
              placeholder="9/10 02:00~03:00 점검이 예정되어 있습니다."
            />
          </Field>
          {error && <p className="form__error">{error}</p>}
          <Button type="submit" loading={submitting}>
            모든 회원에게 발송
          </Button>
        </form>
      ) : (
        <p className="muted">모든 회원에게 알림을 비동기로 발행합니다.</p>
      )}
    </Card>
  )
}
