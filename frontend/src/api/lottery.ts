import type { LotteryEntry, LotteryEvent } from '../types'
import { apiFetch } from './client'

export interface LotteryEventCreateBody {
  title: string
  prize: string
  /** ISO local datetime */
  targetAt: string
  drawAt: string
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

export function listMyLotteryEntries(): Promise<LotteryEntry[]> {
  return apiFetch<LotteryEntry[]>('/api/lottery/events/me')
}
