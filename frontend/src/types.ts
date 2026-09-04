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

export type LotteryEventStatus = 'SCHEDULED' | 'DRAWN'
export type LotteryAudience = 'CURRENT_USERS' | 'ALL_USERS'
export type MyLotteryResult = 'NONE' | 'LOST' | 'WON'

export interface LotteryEvent {
  id: number
  title: string
  prize: string
  audience: LotteryAudience
  winnerCount: number
  status: LotteryEventStatus
  drawnAt: string | null
  entryCount: number
  winners: string[]
  myResult: MyLotteryResult
}

export interface LotteryEntry {
  eventId: number
  eventTitle: string
  prize: string
  winner: boolean
  drawnAt: string | null
}

/** WebSocket `/topic/lottery` 로 오는 추첨 결과. */
export interface LotteryResultEvent {
  eventId: number
  title: string
  prize: string
  winners: string[]
  drawnAt: string
}

export type NotificationType = 'LOTTERY_WON' | 'LOTTERY_LOST' | 'ANNOUNCEMENT'
export type NotificationStatus = 'SENT' | 'FAILED'

/** 알림 한 건. 목록 조회와 WebSocket 푸시(`/topic/notifications/{memberId}`)가 같은 모양이다. */
export interface AppNotification {
  id: number
  type: NotificationType
  title: string
  body: string
  refId: number | null
  status: NotificationStatus
  read: boolean
  createdAt: string
}

export type RankingScope = 'all' | 'daily'

export interface RankingEntry {
  rank: number
  memberId: number
  memberName: string
  minutes: number
}

export interface MyRank {
  /** 아직 랭크에 없으면 null */
  rank: number | null
  minutes: number
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
