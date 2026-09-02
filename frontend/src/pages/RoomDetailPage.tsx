import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { createHold, confirmHold, releaseHold } from '../api/holds'
import { getRoomSchedule } from '../api/rooms'
import { useAuth } from '../auth/AuthContext'
import { ScheduleTypeBadge } from '../components/StatusBadge'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Select, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Hold, RoomSchedule } from '../types'
import {
  DAY_TRACK,
  MAX_SLOT_HOURS,
  SLOT_MINUTES,
  addMinutesIso,
  dayTrackSpan,
  formatTime,
  halfHourStarts,
  toSlotIso,
} from '../utils/format'

const DURATIONS = Array.from({ length: (MAX_SLOT_HOURS * 60) / SLOT_MINUTES }, (_, i) => {
  const mins = (i + 1) * SLOT_MINUTES
  return { mins, label: `${mins}분` }
})

function todayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')}`
}

function overlaps(aStart: string, aEnd: string, bStart: string, bEnd: string): boolean {
  return new Date(aStart) < new Date(bEnd) && new Date(aEnd) > new Date(bStart)
}

export function RoomDetailPage() {
  const { roomId: roomIdParam } = useParams()
  const roomId = Number(roomIdParam)
  const { status: authStatus } = useAuth()
  const toast = useToast()

  const [date, setDate] = useState(todayStr())
  const schedule = useApi<RoomSchedule>(() => getRoomSchedule(roomId, date), [roomId, date])

  const starts = useMemo(halfHourStarts, [])
  const [startTime, setStartTime] = useState('10:00')
  const [durationMins, setDurationMins] = useState(60)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string>()

  const [activeHold, setActiveHold] = useState<Hold | null>(null)

  const startIso = toSlotIso(date, startTime)
  const endIso = addMinutesIso(startIso, durationMins)

  const clientClash = (schedule.data?.entries ?? []).some((e) =>
    overlaps(startIso, endIso, e.startAt, e.endAt),
  )

  const submitHold = async () => {
    setFormError(undefined)
    if (clientClash) {
      setFormError('선택한 시간에 이미 예약이나 홀딩이 있습니다.')
      return
    }
    setSubmitting(true)
    try {
      const hold = await createHold({ roomId, startAt: startIso, endAt: endIso })
      setActiveHold(hold)
      toast.success('홀딩했습니다. 시간 안에 확정하세요.')
      schedule.reload()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : '홀딩에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const confirm = async () => {
    if (!activeHold) return
    try {
      await confirmHold(activeHold.roomId, activeHold.holdId)
      toast.success('예약을 확정했습니다.')
      setActiveHold(null)
      schedule.reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '확정에 실패했습니다.')
      setActiveHold(null)
      schedule.reload()
    }
  }

  const release = async () => {
    if (!activeHold) return
    try {
      await releaseHold(activeHold.roomId, activeHold.holdId)
      toast.info('홀딩을 해제했습니다.')
    } catch {
      /* 이미 만료됐을 수 있음 */
    }
    setActiveHold(null)
    schedule.reload()
  }

  return (
    <div className="page">
      <div className="page__head">
        <Link to="/rooms" className="page__back">
          ← 룸 목록
        </Link>
        <h1>{schedule.data?.roomName ?? `룸 #${roomId}`}</h1>
        <p className="page__lead">
          예약·홀딩 현황입니다. 홀딩은 {''}
          <strong>10분</strong> 뒤 자동 해제되며, 그 안에 확정해야 예약이 됩니다.
        </p>
      </div>

      {activeHold && (
        <HoldBanner hold={activeHold} onConfirm={confirm} onRelease={release} onExpire={release} />
      )}

      <Card
        title="예약 현황"
        actions={
          <Field label="날짜" htmlFor="schedule-date">
            <input
              id="schedule-date"
              className="input"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Field>
        }
      >
        {schedule.loading && <Spinner />}
        {schedule.error && <EmptyState>{schedule.error}</EmptyState>}
        {schedule.data && (
          <DayTimeline entries={schedule.data.entries} />
        )}
      </Card>

      <Card title="홀딩">
        {authStatus !== 'authenticated' ? (
          <EmptyState>
            <Link to="/login">로그인</Link> 후 홀딩할 수 있습니다.
          </EmptyState>
        ) : (
          <div className="form">
            <div className="form__row">
              <Field label="시작">
                <Select value={startTime} onChange={(e) => setStartTime(e.target.value)}>
                  {starts.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="길이">
                <Select
                  value={durationMins}
                  onChange={(e) => setDurationMins(Number(e.target.value))}
                >
                  {DURATIONS.map((d) => (
                    <option key={d.mins} value={d.mins}>
                      {d.label}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <p className="form__foot mono-sm">
              {startTime} – {formatTime(endIso)} · {date}
            </p>
            {formError && <p className="form__error">{formError}</p>}
            <div>
              <Button onClick={submitHold} loading={submitting} disabled={!!activeHold || clientClash}>
                {clientClash ? '시간 겹침' : '홀딩하기'}
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}

function DayTimeline({ entries }: { entries: RoomSchedule['entries'] }) {
  const hours: number[] = []
  for (let h = DAY_TRACK.start; h <= DAY_TRACK.end; h += 2) hours.push(h)

  return (
    <div className="schedule">
      <div className="schedule__track">
        {entries.map((e, i) => {
          const span = dayTrackSpan(e.startAt, e.endAt)
          return (
            <div
              key={i}
              className={`schedule__block schedule__block--${e.type.toLowerCase()}${
                e.mine ? ' is-mine' : ''
              }`}
              style={{ left: `${span.left}%`, width: `${span.width}%` }}
              title={`${e.type} ${formatTime(e.startAt)}–${formatTime(e.endAt)}${
                e.mine ? ' (내 것)' : ''
              }`}
            />
          )
        })}
      </div>
      <div className="schedule__axis">
        {hours.map((h) => (
          <span key={h}>{String(h).padStart(2, '0')}</span>
        ))}
      </div>

      {entries.length === 0 ? (
        <p className="muted">이 날은 예약·홀딩이 없습니다.</p>
      ) : (
        <ul className="schedule__list">
          {entries.map((e, i) => (
            <li key={i}>
              <ScheduleTypeBadge type={e.type} />
              <span className="mono-sm">
                {formatTime(e.startAt)}–{formatTime(e.endAt)}
              </span>
              {e.mine && <span className="muted">내 것</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function HoldBanner({
  hold,
  onConfirm,
  onRelease,
  onExpire,
}: {
  hold: Hold
  onConfirm: () => void
  onRelease: () => void
  onExpire: () => void
}) {
  const [remainingMs, setRemainingMs] = useState(() => new Date(hold.expiresAt).getTime() - Date.now())

  useEffect(() => {
    const tick = () => {
      const left = new Date(hold.expiresAt).getTime() - Date.now()
      setRemainingMs(left)
      if (left <= 0) onExpire()
    }
    tick()
    const id = window.setInterval(tick, 1000)
    return () => window.clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hold.holdId])

  const total = Math.max(Math.ceil(remainingMs / 1000), 0)
  const mm = String(Math.floor(total / 60)).padStart(2, '0')
  const ss = String(total % 60).padStart(2, '0')

  return (
    <div className="hold-banner">
      <div className="hold-banner__info">
        <strong>홀딩 중</strong>
        <span className="mono-sm">
          {formatTime(hold.startAt)}–{formatTime(hold.endAt)}
        </span>
        <span className="hold-banner__count">
          남은 시간 {mm}:{ss}
        </span>
      </div>
      <div className="hold-banner__actions">
        <Button onClick={onConfirm}>확정</Button>
        <Button variant="danger" onClick={onRelease}>
          취소
        </Button>
      </div>
    </div>
  )
}
