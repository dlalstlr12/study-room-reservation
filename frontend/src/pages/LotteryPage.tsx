import { useEffect, useRef, useState } from 'react'
import { listLotteryEvents } from '../api/lottery'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, EmptyState, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import { onConnectionChange } from '../realtime/stompClient'
import { subscribeLottery } from '../realtime/lotteryChannel'
import type { LotteryAudience, LotteryEvent } from '../types'

const AUDIENCE_LABEL: Record<LotteryAudience, string> = {
  CURRENT_USERS: '현재 이용중인 회원',
  ALL_USERS: '전체 회원',
}

const RESULT_LABEL: Record<LotteryEvent['myResult'], string> = {
  WON: '🎉 당첨',
  LOST: '미당첨',
  NONE: '',
}

export function LotteryPage() {
  const { user } = useAuth()
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
  useEffect(() => subscribeLottery((r) => onResult.current(r.winners, r.title)), [])

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
          <strong>현재 이용중인 회원</strong> 또는 <strong>전체 회원</strong> 중에서 추첨합니다. 추첨
          결과는 새로고침 없이 발표되며, 시드를 기록해 결과를 재현·검증할 수 있습니다.
        </p>
      </div>

      {events.loading && <Spinner />}
      {events.error && <EmptyState>{events.error}</EmptyState>}
      {events.data && events.data.length === 0 && <EmptyState>추첨 이벤트가 없습니다.</EmptyState>}
      {events.data && events.data.length > 0 && (
        <div className="grid grid--cards">
          {events.data.map((event) => (
            <LotteryCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  )
}

function LotteryCard({ event }: { event: LotteryEvent }) {
  const [revealed, setRevealed] = useState(false)
  const scheduled = event.status === 'SCHEDULED'

  return (
    <article className={`lottery-card${event.myResult === 'WON' ? ' lottery-card--won' : ''}`}>
      <header className="lottery-card__head">
        <h3>{event.title}</h3>
        <span className={`badge badge--${scheduled ? 'reserved' : 'done'}`}>{event.status}</span>
      </header>
      <p className="lottery-card__prize">🎁 {event.prize}</p>
      <dl className="lottery-card__meta">
        <div>
          <dt>대상</dt>
          <dd className="mono-sm">{AUDIENCE_LABEL[event.audience]}</dd>
        </div>
        <div>
          <dt>응모 / 당첨</dt>
          <dd className="mono-sm">
            {event.entryCount}명 / {event.winnerCount}명
          </dd>
        </div>
      </dl>

      {event.status === 'DRAWN' && (
        <div className="lottery-card__result">
          {event.winners.length === 0 ? (
            <span className="muted">응모자가 없어 당첨자 없음</span>
          ) : !revealed ? (
            <Button variant="secondary" onClick={() => setRevealed(true)}>
              당첨자 확인하기 ({event.winners.length}명)
            </Button>
          ) : (
            <div className="lottery-card__winners">
              <span className="lottery-card__winners-label">당첨자</span>
              {event.winners.map((name, i) => (
                <span key={i} className="lottery-card__winner">
                  {name}
                </span>
              ))}
            </div>
          )}
          {event.myResult !== 'NONE' && (
            <span
              className={`lottery-card__myresult lottery-card__myresult--${event.myResult.toLowerCase()}`}
            >
              {RESULT_LABEL[event.myResult]}
            </span>
          )}
        </div>
      )}
    </article>
  )
}
