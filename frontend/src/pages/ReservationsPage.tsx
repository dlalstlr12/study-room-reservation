import { useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { listRooms } from '../api/rooms'
import { cancelReservation, createReservation, listMyReservations } from '../api/reservations'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Input, Select, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Reservation, ReservationStatus, Room } from '../types'
import { dayTrackSpan, formatDate, formatTime, toLocalInputValue } from '../utils/format'

const MAX_HOURS = 4

const STATUS_OPTIONS: { value: '' | ReservationStatus; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'RESERVED', label: '예약됨' },
  { value: 'CANCELLED', label: '취소됨' },
  { value: 'COMPLETED', label: '이용 완료' },
]

function defaultRange() {
  const start = new Date()
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  return { start: toLocalInputValue(start), end: toLocalInputValue(end) }
}

export function ReservationsPage() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const presetRoomId = searchParams.get('roomId') ?? ''

  const rooms = useApi<Room[]>(() => listRooms(), [])
  const [filter, setFilter] = useState<'' | ReservationStatus>('')
  const reservations = useApi<Reservation[]>(
    () => listMyReservations(filter || undefined),
    [filter],
  )

  const range = useMemo(defaultRange, [])
  const [roomId, setRoomId] = useState(presetRoomId)
  const [startAt, setStartAt] = useState(range.start)
  const [endAt, setEndAt] = useState(range.end)
  const [formError, setFormError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setFormError(undefined)

    if (!roomId) return setFormError('룸을 선택하세요.')
    const start = new Date(startAt)
    const end = new Date(endAt)
    if (!(start < end)) return setFormError('시작 시각이 종료 시각보다 앞서야 합니다.')
    if (start.getTime() <= Date.now()) return setFormError('시작 시각은 미래여야 합니다.')
    if (end.getTime() - start.getTime() > MAX_HOURS * 3600_000)
      return setFormError(`1회 예약은 최대 ${MAX_HOURS}시간까지 가능합니다.`)

    setSubmitting(true)
    try {
      await createReservation({ roomId: Number(roomId), startAt, endAt })
      toast.success('예약했습니다.')
      reservations.reload()
    } catch (err) {
      if (err instanceof ApiError) {
        setFormError(err.message)
      } else {
        setFormError('예약 생성에 실패했습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleCancel = async (r: Reservation) => {
    if (!window.confirm('이 예약을 취소할까요?')) return
    try {
      await cancelReservation(r.id)
      toast.success('예약을 취소했습니다.')
      reservations.reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '취소에 실패했습니다.')
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <h1>내 예약</h1>
        <p className="page__lead">
          예약 생성은 동시성 제어가 없습니다 (로드맵 2단계에서 락 도입 예정).
        </p>
      </div>

      <Card title="새 예약">
        <form className="form" onSubmit={submit} noValidate>
          <Field label="룸">
            <Select value={roomId} onChange={(e) => setRoomId(e.target.value)}>
              <option value="">룸 선택…</option>
              {(rooms.data ?? []).map((room) => (
                <option key={room.id} value={room.id}>
                  {room.name} (정원 {room.capacity})
                </option>
              ))}
            </Select>
          </Field>
          <div className="form__row">
            <Field label="시작">
              <Input
                type="datetime-local"
                value={startAt}
                onChange={(e) => setStartAt(e.target.value)}
              />
            </Field>
            <Field label="종료">
              <Input
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
              />
            </Field>
          </div>
          {formError && <p className="form__error">{formError}</p>}
          <Button type="submit" loading={submitting}>
            예약하기
          </Button>
        </form>
      </Card>

      <Card
        title="예약 목록"
        actions={
          <div className="filter">
            <label htmlFor="res-filter">상태</label>
            <Select
              id="res-filter"
              value={filter}
              onChange={(e) => setFilter(e.target.value as '' | ReservationStatus)}
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </Select>
          </div>
        }
      >
        {reservations.loading && <Spinner />}
        {reservations.error && <EmptyState>{reservations.error}</EmptyState>}
        {reservations.data && reservations.data.length === 0 && (
          <EmptyState>아직 예약한 룸이 없습니다. 룸 목록에서 시간을 고르세요.</EmptyState>
        )}
        {reservations.data && reservations.data.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>룸</th>
                <th>날짜</th>
                <th>시간대 · 08–24시</th>
                <th>상태</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {reservations.data.map((r) => {
                const span = dayTrackSpan(r.startAt, r.endAt)
                const done = r.status !== 'RESERVED'
                return (
                  <tr key={r.id}>
                    <td>{r.roomName}</td>
                    <td className="table__time">{formatDate(r.startAt)}</td>
                    <td>
                      <span className="table__time">
                        {formatTime(r.startAt)}–{formatTime(r.endAt)}
                      </span>
                      <span className={`timebar${done ? ' timebar--done' : ''}`}>
                        <span
                          className="timebar__fill"
                          style={{ left: `${span.left}%`, width: `${span.width}%` }}
                        />
                      </span>
                    </td>
                    <td>
                      <ReservationStatusBadge status={r.status} />
                    </td>
                    <td className="table__actions">
                      {r.status === 'RESERVED' && (
                        <Button variant="danger" onClick={() => handleCancel(r)}>
                          취소
                        </Button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  )
}
