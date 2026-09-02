import type { LotteryAudience, LotteryEntry, LotteryEvent } from '../types'
import { apiFetch } from './client'

export interface LotteryEventCreateBody {
  title: string
  prize: string
  audience: LotteryAudience
  winnerCount: number
}

export function listLotteryEvents(): Promise<LotteryEvent[]> {
  return apiFetch<LotteryEvent[]>('/api/lottery/events')
}

export function getLotteryEvent(id: number): Promise<LotteryEvent> {
  return apiFetch<LotteryEvent>(`/api/lottery/events/${id}`)
}

export function createLotteryEvent(body: LotteryEventCreateBody): Promise<LotteryEvent> {
  return apiFetch<LotteryEvent>('/api/lottery/events', { method: 'POST', body })
}

export function drawLotteryEvent(id: number): Promise<LotteryEvent> {
  return apiFetch<LotteryEvent>(`/api/lottery/events/${id}/draw`, { method: 'POST' })
}

export function deleteLotteryEvent(id: number): Promise<void> {
  return apiFetch<void>(`/api/lottery/events/${id}`, { method: 'DELETE' })
}

export function listMyLotteryEntries(): Promise<LotteryEntry[]> {
  return apiFetch<LotteryEntry[]>('/api/lottery/events/me')
}
