import type { HealthResponse } from '../types'
import { apiFetch } from './client'

export function getHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>('/api/health', { anonymous: true })
}
