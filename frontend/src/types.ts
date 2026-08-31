export type MemberRole = 'USER' | 'ADMIN'
export type RoomStatus = 'AVAILABLE' | 'HOLDING' | 'OCCUPIED'
export type ReservationStatus = 'RESERVED' | 'CANCELLED' | 'COMPLETED'

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
  status: RoomStatus
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
