import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createLotteryEvent, drawLotteryEvent, listLotteryEvents } from '../api/lottery'
import type { LotteryEventCreateBody } from '../api/lottery'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Input, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import { onConnectionChange } from '../realtime/stompClient'
import { subscribeLottery } from '../realtime/lotteryChannel'
import type { LotteryEvent } from '../types'
import { formatDateTime } from '../utils/format'

function useCountdown(targetIso: string): string {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [])
  const ms = new Date(targetIso).getTime() - now
  if (ms <= 0) return '추첨 대기 중…'
  const total = Math.floor(ms / 1000)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return h > 0 ? `${h}시간 ${m}분 후` : `${m}:${String(s).padStart(2, '0')} 후`
}

const RESULT_LABEL: Record<LotteryEvent['myResult'], string> = {
  WON: '🎉 당첨',
  LOST: '미당첨',
  NONE: '',
}

export function LotteryPage() {
  const { isAdmin, user } = useAuth()
  const toast = useToast()
  const events = useApi<LotteryEvent[]>(() => listLotteryEvents(), [])

  const [live, setLive] = useState(false)
  useEffect(() => onConnectionChange(setLive), [])

  const onResult = useRef<(winners: string[], title: string) => void>(() => {})
  onResult.current = (winners, title) => {
    events.reload()
    if (user && winners.includes(user.name)) {
      toast.success(`🎉 "${title}" 당첨되셨습니다!`)
    } else {
      toast.info(`추첨 완료: ${title}`)
    }
  }
  useEffect(
    () => subscribeLottery((r) => onResult.current(r.winners, r.title)),
    [],
  )

  const [creating, setCreating] = useState(false)
  const handleCreate = async (body: LotteryEventCreateBody) => {
    await createLotteryEvent(body)
    toast.success('추첨 이벤트를 만들었습니다.')
    setCreating(false)
    events.reload()
  }

  const handleDraw = async (event: LotteryEvent) => {
    try {
      await drawLotteryEvent(event.id)
      toast.success('추첨했습니다.')
      events.reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '추첨에 실패했습니다.')
      events.reload()
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <div className="page__head-row">
          <h1>이벤트 추첨</h1>
          <span className={`live live--${live ? 'on' : 'off'}`} title={live ? '실시간 연결됨' : '연결 끊김'}>
            실시간
          </span>
        </div>
        <p className="page__lead">
          <strong>추첨 기준 시각</strong>에 룸을 이용 중이던 회원이 응모됩니다. 추첨 결과는 새로고침 없이
          발표됩니다. 시드를 기록해 결과를 재현·검증할 수 있습니다.
        </p>
      </div>

      {isAdmin && (
        <Card
          title="새 추첨 이벤트"
          actions={
            <Button variant={creating ? 'ghost' : 'secondary'} onClick={() => setCreating((v) => !v)}>
              {creating ? '닫기' : '새 이벤트'}
            </Button>
          }
        >
          {creating ? (
            <LotteryForm onSubmit={handleCreate} />
          ) : (
            <p className="muted">"새 이벤트" 버튼으로 폼을 엽니다.</p>
          )}
        </Card>
      )}

      {events.loading && <Spinner />}
      {events.error && <EmptyState>{events.error}</EmptyState>}
      {events.data && events.data.length === 0 && (
        <EmptyState>추첨 이벤트가 없습니다.</EmptyState>
      )}
      {events.data && events.data.length > 0 && (
        <div className="grid grid--cards">
          {events.data.map((event) => (
            <LotteryCard key={event.id} event={event} isAdmin={isAdmin} onDraw={handleDraw} />
          ))}
        </div>
      )}
    </div>
  )
}

function LotteryCard({
  event,
  isAdmin,
  onDraw,
}: {
  event: LotteryEvent
  isAdmin: boolean
  onDraw: (event: LotteryEvent) => void
}) {
  const countdown = useCountdown(event.drawAt)
  const scheduled = event.status === 'SCHEDULED'

  return (
    <article className={`lottery-card${event.myResult === 'WON' ? ' lottery-card--won' : ''}`}>
      <header className="lottery-card__head">
        <h3>{event.title}</h3>
        <span className={`badge badge--${scheduled ? 'free' : 'done'}`}>{event.status}</span>
      </header>
      <p className="lottery-card__prize">🎁 {event.prize}</p>
      <dl className="lottery-card__meta">
        <div>
          <dt>기준 시각</dt>
          <dd className="mono-sm">{formatDateTime(event.targetAt)}</dd>
        </div>
        <div>
          <dt>{scheduled ? '추첨' : '추첨됨'}</dt>
          <dd className="mono-sm">
            {scheduled ? countdown : formatDateTime(event.drawnAt ?? event.drawAt)}
          </dd>
        </div>
        <div>
          <dt>응모 / 당첨</dt>
          <dd className="mono-sm">
            {event.entryCount}명 / {event.winnerCount}명
          </dd>
        </div>
      </dl>

      {event.status === 'DRAWN' && (
        <div className="lottery-card__winners">
          {event.winners.length === 0 ? (
            <span className="muted">응모자가 없어 당첨자 없음</span>
          ) : (
            <>
              <span className="lottery-card__winners-label">당첨자</span>
              {event.winners.map((name, i) => (
                <span key={i} className="lottery-card__winner">
                  {name}
                </span>
              ))}
            </>
          )}
          {event.myResult !== 'NONE' && (
            <span className={`lottery-card__myresult lottery-card__myresult--${event.myResult.toLowerCase()}`}>
              {RESULT_LABEL[event.myResult]}
            </span>
          )}
        </div>
      )}

      {isAdmin && scheduled && (
        <footer className="lottery-card__foot">
          <Button onClick={() => onDraw(event)}>지금 추첨</Button>
        </footer>
      )}
    </article>
  )
}

function LotteryForm({ onSubmit }: { onSubmit: (body: LotteryEventCreateBody) => Promise<void> }) {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [prize, setPrize] = useState('')
  const [targetAt, setTargetAt] = useState('')
  const [drawAt, setDrawAt] = useState('')
  const [winnerCount, setWinnerCount] = useState('1')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    if (!title.trim() || !prize.trim() || !targetAt || !drawAt) {
      setError('모든 항목을 입력하세요.')
      return
    }
    if (new Date(drawAt).getTime() <= Date.now()) {
      setError('추첨 시각은 미래여야 합니다.')
      return
    }
    setSubmitting(true)
    try {
      await onSubmit({
        title: title.trim(),
        prize: prize.trim(),
        targetAt: `${targetAt}:00`,
        drawAt: `${drawAt}:00`,
        winnerCount: Number(winnerCount),
      })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '생성에 실패했습니다.')
      if (!(err instanceof ApiError)) toast.error('생성에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={submit} noValidate>
      <Field label="제목">
        <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="티타임 추첨" />
      </Field>
      <Field label="상품">
        <Input value={prize} onChange={(e) => setPrize(e.target.value)} placeholder="아메리카노 기프티콘" />
      </Field>
      <div className="form__row">
        <Field label="기준 시각 (이 시각 이용자가 응모)">
          <Input type="datetime-local" value={targetAt} onChange={(e) => setTargetAt(e.target.value)} />
        </Field>
        <Field label="추첨 시각">
          <Input type="datetime-local" value={drawAt} onChange={(e) => setDrawAt(e.target.value)} />
        </Field>
      </div>
      <Field label="당첨 인원">
        <Input
          type="number"
          min={1}
          value={winnerCount}
          onChange={(e) => setWinnerCount(e.target.value)}
        />
      </Field>
      {error && <p className="form__error">{error}</p>}
      <Button type="submit" loading={submitting}>
        만들기
      </Button>
    </form>
  )
}
