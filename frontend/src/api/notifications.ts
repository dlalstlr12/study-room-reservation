import type { AppNotification } from '../types'
import { apiFetch } from './client'

export function listNotifications(unreadOnly = false): Promise<AppNotification[]> {
  return apiFetch<AppNotification[]>('/api/notifications', {
    query: { unreadOnly: unreadOnly ? 'true' : undefined },
  })
}

export function getUnreadCount(): Promise<{ count: number }> {
  return apiFetch<{ count: number }>('/api/notifications/unread-count')
}

export function markNotificationRead(id: number): Promise<void> {
  return apiFetch<void>(`/api/notifications/${id}/read`, { method: 'POST' })
}

export function markAllNotificationsRead(): Promise<void> {
  return apiFetch<void>('/api/notifications/read-all', { method: 'POST' })
}

export function sendAnnouncement(body: { title: string; body: string }): Promise<void> {
  return apiFetch<void>('/api/notifications/announcements', { method: 'POST', body })
}
