export type MemberRole = 'USER' | 'ADMIN'
export type ReservationStatus = 'RESERVED' | 'CANCELLED' | 'COMPLETED'
export type ScheduleEntryType = 'RESERVED' | 'HOLDING'

export interface Member {
  id: number
  email: string
  name: string
  role: MemberRole
  createdAt: string
}

export interface Room {
  id: number
  name: string
  capacity: number
  description: string | null
}

export interface Reservation {
  id: number
  roomId: number
  roomName: string
  memberId: number
  startAt: string
  endAt: string
  status: ReservationStatus
  createdAt: string
}

export interface Hold {
  holdId: string
  roomId: number
  roomName: string
  startAt: string
  endAt: string
  /** ISO — 이 시각이 지나면 홀딩이 자동 해제된다 */
  expiresAt: string
}

export interface RoomScheduleEntry {
  type: ScheduleEntryType
  startAt: string
  endAt: string
  /** 로그인한 사용자 본인의 예약/홀딩인지 */
  mine: boolean
}

export interface RoomSchedule {
  roomId: number
  roomName: string
  date: string
  entries: RoomScheduleEntry[]
}

/** WebSocket `/topic/rooms/{id}` 로 오는 실시간 알림. "이 룸이 바뀌었다"만 알린다. */
export interface RoomChangeEvent {
  roomId: number
  /** 변경을 일으킨 회원. 홀딩 만료·백스톱은 null */
  actorMemberId: number | null
  at: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  accessTokenExpiresInMs: number
}

export interface FieldError {
  field: string
  reason: string
}

export interface ErrorResponse {
  code: string
  message: string
  fieldErrors: FieldError[]
}

export interface HealthResponse {
  status: string
  timestamp: string
}
