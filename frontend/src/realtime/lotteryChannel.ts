import type { LotteryResultEvent } from '../types'
import { subscribe } from './stompClient'

/** 전역 추첨 발표 피드. */
export function subscribeLottery(onResult: (result: LotteryResultEvent) => void): () => void {
  return subscribe('/topic/lottery', (data) => onResult(data as LotteryResultEvent))
}

/** 특정 이벤트의 추첨 발표. */
export function subscribeLotteryEvent(
  eventId: number,
  onResult: (result: LotteryResultEvent) => void,
): () => void {
  return subscribe(`/topic/lottery/${eventId}`, (data) => onResult(data as LotteryResultEvent))
}
