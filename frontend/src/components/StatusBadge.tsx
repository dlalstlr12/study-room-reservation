import type { ReservationStatus, RoomStatus } from '../types'

const ROOM_LABEL: Record<RoomStatus, string> = {
  AVAILABLE: '예약 가능',
  HOLDING: '홀딩 중',
  OCCUPIED: '사용 중',
}

const RESERVATION_LABEL: Record<ReservationStatus, string> = {
  RESERVED: '예약됨',
  CANCELLED: '취소됨',
  COMPLETED: '이용 완료',
}

const TONE: Record<RoomStatus | ReservationStatus, 'green' | 'gray' | 'red'> = {
  AVAILABLE: 'green',
  RESERVED: 'green',
  HOLDING: 'gray',
  COMPLETED: 'gray',
  OCCUPIED: 'red',
  CANCELLED: 'red',
}

export function RoomStatusBadge({ status }: { status: RoomStatus }) {
  return (
    <span className={`badge badge--${TONE[status]}`} title={ROOM_LABEL[status]}>
      {status}
    </span>
  )
}

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <span className={`badge badge--${TONE[status]}`} title={RESERVATION_LABEL[status]}>
      {status}
    </span>
  )
}
