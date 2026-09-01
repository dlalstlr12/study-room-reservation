import type { ReservationStatus, ScheduleEntryType } from '../types'

const RESERVATION_LABEL: Record<ReservationStatus, string> = {
  RESERVED: '예약됨',
  CANCELLED: '취소됨',
  COMPLETED: '이용 완료',
}

/** tone maps to a door-panel lamp colour */
type Tone = 'free' | 'reserved' | 'stop' | 'done'

const RESERVATION_TONE: Record<ReservationStatus, Tone> = {
  RESERVED: 'reserved',
  COMPLETED: 'done',
  CANCELLED: 'done',
}

const SCHEDULE_LABEL: Record<ScheduleEntryType, string> = {
  RESERVED: '예약',
  HOLDING: '홀딩',
}

const SCHEDULE_TONE: Record<ScheduleEntryType, Tone> = {
  RESERVED: 'reserved',
  HOLDING: 'free',
}

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <span className={`badge badge--${RESERVATION_TONE[status]}`} title={RESERVATION_LABEL[status]}>
      {status}
    </span>
  )
}

export function ScheduleTypeBadge({ type }: { type: ScheduleEntryType }) {
  return (
    <span className={`badge badge--${SCHEDULE_TONE[type]}`} title={SCHEDULE_LABEL[type]}>
      {type}
    </span>
  )
}
