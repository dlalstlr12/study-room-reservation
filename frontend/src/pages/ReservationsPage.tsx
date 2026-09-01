import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { confirmHold, listMyHolds, releaseHold } from '../api/holds'
import { cancelReservation, listMyReservations } from '../api/reservations'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Select, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Hold, Reservation, ReservationStatus } from '../types'
import { dayTrackSpan, formatDate, formatTime } from '../utils/format'

const STATUS_OPTIONS: { value: '' | ReservationStatus; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'RESERVED', label: '예약됨' },
  { value: 'CANCELLED', label: '취소됨' },
  { value: 'COMPLETED', label: '이용 완료' },
]

export function ReservationsPage() {
  const toast = useToast()
  const [filter, setFilter] = useState<'' | ReservationStatus>('')

  const holds = useApi<Hold[]>(() => listMyHolds(), [])
  const reservations = useApi<Reservation[]>(() => listMyReservations(filter || undefined), [filter])

  const handleConfirm = async (h: Hold) => {
    try {
      await confirmHold(h.roomId, h.holdId)
      toast.success('예약을 확정했습니다.')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '확정에 실패했습니다.')
    }
    holds.reload()
    reservations.reload()
  }

  const handleRelease = async (h: Hold) => {
    try {
      await releaseHold(h.roomId, h.holdId)
      toast.info('홀딩을 해제했습니다.')
    } catch {
      /* 이미 만료됐을 수 있음 */
    }
    holds.reload()
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
          예약은 <Link to="/rooms">룸</Link>에서 시간을 홀딩한 뒤 확정합니다. 홀딩은 10분 뒤 자동 해제됩니다.
        </p>
      </div>

      <Card title="내 홀딩">
        {holds.loading && <Spinner />}
        {holds.data && holds.data.length === 0 && (
          <EmptyState>진행 중인 홀딩이 없습니다.</EmptyState>
        )}
        {holds.data && holds.data.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>룸</th>
                <th>시간</th>
                <th>만료</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {holds.data.map((h) => (
                <tr key={h.holdId}>
                  <td>{h.roomName}</td>
                  <td className="table__time">
                    {formatDate(h.startAt)} {formatTime(h.startAt)}–{formatTime(h.endAt)}
                  </td>
                  <td className="table__time">{formatTime(h.expiresAt)}</td>
                  <td className="table__actions">
                    <Button onClick={() => handleConfirm(h)}>확정</Button>
                    <Button variant="danger" onClick={() => handleRelease(h)}>
                      해제
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
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
          <EmptyState>아직 예약이 없습니다. 룸에서 시간을 골라 홀딩하세요.</EmptyState>
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
