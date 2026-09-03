import type { AppNotification } from '../types'
import { subscribe } from './stompClient'

/** 내 알림 실시간 수신. 워커가 저장 직후 `/topic/notifications/{memberId}` 로 푸시한다. */
export function subscribeNotifications(
  memberId: number,
  onNotification: (notification: AppNotification) => void,
): () => void {
  return subscribe(`/topic/notifications/${memberId}`, (data) =>
    onNotification(data as AppNotification),
  )
}

export { onConnectionChange } from './stompClient'
