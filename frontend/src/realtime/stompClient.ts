import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const BROKER_URL = API_BASE_URL.replace(/^http/, 'ws') + '/ws'

type JsonHandler = (data: unknown) => void
type ConnectionHandler = (connected: boolean) => void

const handlers = new Map<string, Set<JsonHandler>>()
const subs = new Map<string, StompSubscription>()
const connectionHandlers = new Set<ConnectionHandler>()
let connected = false

const client = new Client({
  brokerURL: BROKER_URL,
  reconnectDelay: 3000,
  onConnect: () => {
    connected = true
    connectionHandlers.forEach((cb) => cb(true))
    handlers.forEach((_, destination) => openSubscription(destination))
  },
  onWebSocketClose: () => {
    connected = false
    subs.clear()
    connectionHandlers.forEach((cb) => cb(false))
  },
})

function openSubscription(destination: string) {
  if (!client.connected || subs.has(destination)) return
  const sub = client.subscribe(destination, (message: IMessage) => {
    let data: unknown
    try {
      data = JSON.parse(message.body)
    } catch {
      return
    }
    handlers.get(destination)?.forEach((cb) => cb(data))
  })
  subs.set(destination, sub)
}

/** STOMP destination을 구독한다. 반환 함수를 호출하면 해지된다. destination별 ref-count. */
export function subscribe(destination: string, onJson: JsonHandler): () => void {
  let set = handlers.get(destination)
  if (!set) {
    set = new Set()
    handlers.set(destination, set)
  }
  set.add(onJson)

  if (!client.active) {
    client.activate()
  } else {
    openSubscription(destination)
  }

  return () => {
    const current = handlers.get(destination)
    if (!current) return
    current.delete(onJson)
    if (current.size > 0) return
    handlers.delete(destination)
    subs.get(destination)?.unsubscribe()
    subs.delete(destination)
    if (handlers.size === 0) {
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
