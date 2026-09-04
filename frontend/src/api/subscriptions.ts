import type { Payment, Subscription } from '../types'
import { apiFetch } from './client'

export function getMySubscription(): Promise<Subscription> {
  return apiFetch<Subscription>('/api/subscriptions/me')
}

export function subscribePro(): Promise<Subscription> {
  return apiFetch<Subscription>('/api/subscriptions', { method: 'POST' })
}

export function cancelSubscription(): Promise<Subscription> {
  return apiFetch<Subscription>('/api/subscriptions/cancel', { method: 'POST' })
}

export function listMyPayments(): Promise<Payment[]> {
  return apiFetch<Payment[]>('/api/subscriptions/me/payments')
}

export function runBilling(): Promise<{ jobExecutionId: number; status: string }> {
  return apiFetch<{ jobExecutionId: number; status: string }>('/api/admin/billing/run', {
    method: 'POST',
  })
}
