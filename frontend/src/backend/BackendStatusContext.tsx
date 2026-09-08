import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { getHealth } from '../api/health'

export type BackendState = 'checking' | 'up' | 'down'

export interface BackendStatus {
  state: BackendState
  /** 서버를 기다리기 시작한 뒤 경과 시간(ms). 응답이 오면 0으로 돌아간다 */
  waitingMs: number
  /** 서버가 살아난 순간마다 증가한다. 화면이 재조회 시점을 잡는 데 쓴다 */
  readyCount: number
}

/**
 * 헬스 체크를 한곳에서 돌리고 결과를 화면에 공유한다.
 *
 * <p>무료 티어(Render)는 유휴 15분이면 인스턴스를 내리고, 다음 요청에 다시 기동하는 데
 * 몇 분이 걸린다. 그동안 요청은 <b>실패하지 않고 그냥 매달려 있어서</b> 타임아웃을 걸지 않으면
 * 화면이 "확인 중"에서 영영 벗어나지 못한다. 그래서 시도마다 끊고 짧은 간격으로 다시 물어본다.
 */
const PROBE_TIMEOUT_MS = 8_000
/** 기동 대기 중 재시도 간격 */
const RETRY_DELAY_MS = 3_000
/** 정상일 때 감시 간격 */
const HEALTHY_POLL_MS = 15_000

const BackendStatusContext = createContext<BackendStatus>({
  state: 'checking',
  waitingMs: 0,
  readyCount: 0,
})

export function useBackendStatus(): BackendStatus {
  return useContext(BackendStatusContext)
}

export function BackendStatusProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<BackendState>('checking')
  const [waitingMs, setWaitingMs] = useState(0)
  const [readyCount, setReadyCount] = useState(0)
  const waitingSince = useRef<number | null>(Date.now())
  const wasUp = useRef(false)

  useEffect(() => {
    let active = true
    let probeTimer: number | undefined

    const probe = async () => {
      const controller = new AbortController()
      const cutoff = window.setTimeout(() => controller.abort(), PROBE_TIMEOUT_MS)
      try {
        await getHealth(controller.signal)
        if (!active) return
        waitingSince.current = null
        setWaitingMs(0)
        if (!wasUp.current) {
          wasUp.current = true
          setReadyCount((n) => n + 1)
        }
        setState('up')
        probeTimer = window.setTimeout(probe, HEALTHY_POLL_MS)
      } catch {
        if (!active) return
        wasUp.current = false
        if (waitingSince.current === null) waitingSince.current = Date.now()
        setState('down')
        probeTimer = window.setTimeout(probe, RETRY_DELAY_MS)
      } finally {
        window.clearTimeout(cutoff)
      }
    }

    const tick = window.setInterval(() => {
      if (!active) return
      const since = waitingSince.current
      setWaitingMs(since === null ? 0 : Date.now() - since)
    }, 1_000)

    void probe()

    return () => {
      active = false
      window.clearTimeout(probeTimer)
      window.clearInterval(tick)
    }
  }, [])

  return (
    <BackendStatusContext.Provider value={{ state, waitingMs, readyCount }}>
      {children}
    </BackendStatusContext.Provider>
  )
}
