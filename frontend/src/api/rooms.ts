import type { Room, RoomSchedule } from '../types'
import { apiFetch } from './client'

export interface RoomCreateBody {
  name: string
  capacity: number
  description: string
}

export type RoomUpdateBody = RoomCreateBody

export function listRooms(): Promise<Room[]> {
  return apiFetch<Room[]>('/api/rooms', { anonymous: true })
}

export function getRoom(id: number): Promise<Room> {
  return apiFetch<Room>(`/api/rooms/${id}`, { anonymous: true })
}

/** date: 'YYYY-MM-DD' */
export function getRoomSchedule(id: number, date: string): Promise<RoomSchedule> {
  return apiFetch<RoomSchedule>(`/api/rooms/${id}/schedule`, { anonymous: true, query: { date } })
}

export function createRoom(body: RoomCreateBody): Promise<Room> {
  return apiFetch<Room>('/api/rooms', { method: 'POST', body })
}

export function updateRoom(id: number, body: RoomUpdateBody): Promise<Room> {
  return apiFetch<Room>(`/api/rooms/${id}`, { method: 'PUT', body })
}

export function deleteRoom(id: number): Promise<void> {
  return apiFetch<void>(`/api/rooms/${id}`, { method: 'DELETE' })
}
