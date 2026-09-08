import type { HealthResponse } from '../types'
import { apiFetch } from './client'

export function getHealth(signal?: AbortSignal): Promise<HealthResponse> {
  return apiFetch<HealthResponse>('/api/health', { anonymous: true, signal })
}
