import type { RoomChangeEvent } from '../types'
import { onConnectionChange, subscribe } from './stompClient'

/** 룸 변경 이벤트를 구독한다. 반환한 함수를 호출하면 해지된다. */
export function subscribeRoom(roomId: number, onEvent: (event: RoomChangeEvent) => void): () => void {
  return subscribe(`/topic/rooms/${roomId}`, (data) => onEvent(data as RoomChangeEvent))
}

export { onConnectionChange }
