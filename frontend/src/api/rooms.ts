import type { Room, RoomStatus } from '../types'
import { apiFetch } from './client'

export interface RoomCreateBody {
  name: string
  capacity: number
  description: string
}

export interface RoomUpdateBody extends RoomCreateBody {
  status: RoomStatus
}

export function listRooms(status?: RoomStatus): Promise<Room[]> {
  return apiFetch<Room[]>('/api/rooms', { anonymous: true, query: { status } })
}

export function getRoom(id: number): Promise<Room> {
  return apiFetch<Room>(`/api/rooms/${id}`, { anonymous: true })
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
