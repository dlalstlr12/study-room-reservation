import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import type { RoomChangeEvent } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const BROKER_URL = API_BASE_URL.replace(/^http/, 'ws') + '/ws'

type EventHandler = (event: RoomChangeEvent) => void
type ConnectionHandler = (connected: boolean) => void

const roomHandlers = new Map<number, Set<EventHandler>>()
const roomSubs = new Map<number, StompSubscription>()
const connectionHandlers = new Set<ConnectionHandler>()
let connected = false

const client = new Client({
  brokerURL: BROKER_URL,
  reconnectDelay: 3000,
  onConnect: () => {
    connected = true
    connectionHandlers.forEach((cb) => cb(true))
    roomHandlers.forEach((_, roomId) => openSubscription(roomId))
  },
  onWebSocketClose: () => {
    connected = false
    roomSubs.clear()
    connectionHandlers.forEach((cb) => cb(false))
  },
})

function destination(roomId: number): string {
  return `/topic/rooms/${roomId}`
}

function openSubscription(roomId: number) {
  if (!client.connected || roomSubs.has(roomId)) return
  const sub = client.subscribe(destination(roomId), (message: IMessage) => {
    let event: RoomChangeEvent
    try {
      event = JSON.parse(message.body)
    } catch {
      return
    }
    roomHandlers.get(roomId)?.forEach((cb) => cb(event))
  })
  roomSubs.set(roomId, sub)
}

/** 룸 변경 이벤트를 구독한다. 반환한 함수를 호출하면 해지된다. */
export function subscribeRoom(roomId: number, onEvent: EventHandler): () => void {
  let handlers = roomHandlers.get(roomId)
  if (!handlers) {
    handlers = new Set()
    roomHandlers.set(roomId, handlers)
  }
  handlers.add(onEvent)

  if (!client.active) {
    client.activate()
  } else {
    openSubscription(roomId)
  }

  return () => {
    const set = roomHandlers.get(roomId)
    if (!set) return
    set.delete(onEvent)
    if (set.size > 0) return
    roomHandlers.delete(roomId)
    roomSubs.get(roomId)?.unsubscribe()
    roomSubs.delete(roomId)
    if (roomHandlers.size === 0) {
      void client.deactivate()
    }
  }
}

/** WebSocket 연결 상태 변화를 구독한다(현재 상태로 즉시 1회 호출). */
export function onConnectionChange(cb: ConnectionHandler): () => void {
  connectionHandlers.add(cb)
  cb(connected)
  return () => {
    connectionHandlers.delete(cb)
  }
}
