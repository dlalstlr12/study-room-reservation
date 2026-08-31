import type { Reservation, ReservationStatus } from '../types'
import { apiFetch } from './client'

export interface ReservationCreateBody {
  roomId: number
  /** ISO local datetime, e.g. 2026-09-05T10:00 */
  startAt: string
  endAt: string
}

export function createReservation(body: ReservationCreateBody): Promise<Reservation> {
  return apiFetch<Reservation>('/api/reservations', { method: 'POST', body })
}

export function listMyReservations(status?: ReservationStatus): Promise<Reservation[]> {
  return apiFetch<Reservation[]>('/api/reservations/me', { query: { status } })
}

export function getReservation(id: number): Promise<Reservation> {
  return apiFetch<Reservation>(`/api/reservations/${id}`)
}

export function cancelReservation(id: number): Promise<Reservation> {
  return apiFetch<Reservation>(`/api/reservations/${id}/cancel`, { method: 'POST' })
}
