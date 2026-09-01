import type { Hold, Reservation } from '../types'
import { apiFetch } from './client'

export interface HoldCreateBody {
  roomId: number
  /** ISO local datetime, 30분 정렬, e.g. 2026-09-05T10:00:00 */
  startAt: string
  endAt: string
}

export function createHold(body: HoldCreateBody): Promise<Hold> {
  return apiFetch<Hold>('/api/reservations/holds', { method: 'POST', body })
}

export function confirmHold(roomId: number, holdId: string): Promise<Reservation> {
  return apiFetch<Reservation>(`/api/reservations/holds/${roomId}/${holdId}/confirm`, {
    method: 'POST',
  })
}

export function releaseHold(roomId: number, holdId: string): Promise<void> {
  return apiFetch<void>(`/api/reservations/holds/${roomId}/${holdId}`, { method: 'DELETE' })
}

export function listMyHolds(): Promise<Hold[]> {
  return apiFetch<Hold[]>('/api/reservations/holds/me')
}
