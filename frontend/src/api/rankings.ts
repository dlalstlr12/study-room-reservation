import type { MyRank, RankingEntry, RankingScope } from '../types'
import { apiFetch } from './client'

export function getRankings(scope: RankingScope, limit = 20): Promise<RankingEntry[]> {
  return apiFetch<RankingEntry[]>('/api/rankings', { query: { scope, limit } })
}

export function getMyRank(scope: RankingScope): Promise<MyRank> {
  return apiFetch<MyRank>('/api/rankings/me', { query: { scope } })
}

export function rebuildRankings(): Promise<void> {
  return apiFetch<void>('/api/rankings/rebuild', { method: 'POST' })
}
