import { useBackendStatus } from '../backend/BackendStatusContext'

/** 이 시간 안에 응답이 오면 안내를 띄우지 않는다 (이미 깨어 있는 경우) */
const GRACE_MS = 6_000
/** 무료 티어 기동에 보통 걸리는 시간 */
const EXPECTED_MS = 5 * 60 * 1_000

const REPO_URL = 'https://github.com/dlalstlr12/study-room-reservation'

function formatElapsed(ms: number): string {
  const total = Math.floor(ms / 1000)
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return minutes > 0 ? `${minutes}분 ${seconds}초` : `${seconds}초`
}

/**
 * 무료 티어 콜드 스타트 안내.
 *
 * <p>서버가 이미 깨어 있으면 렌더되지 않는다. 기다림이 길어질 때만 나타나서
 * "왜 기다리는지 · 얼마나 더 기다리면 되는지"를 알려준다.
 */
export function ColdStartNotice() {
  const { state, waitingMs } = useBackendStatus()

  if (state === 'up' || waitingMs < GRACE_MS) return null

  const overdue = waitingMs > EXPECTED_MS
  const progress = Math.min(96, Math.round((waitingMs / EXPECTED_MS) * 100))

  return (
    <section className="wakeup" role="status" aria-live="polite">
      <div className="wakeup__head">
        <span className="wakeup__spinner" aria-hidden="true" />
        <strong className="wakeup__title">
          {overdue ? '기동이 예상보다 길어지고 있습니다' : '데모 서버를 깨우는 중입니다'}
        </strong>
        <span className="wakeup__elapsed">{formatElapsed(waitingMs)} 경과</span>
      </div>

      <p className="wakeup__desc">
        이 데모는 <strong>비용 최소화를 위해 무료 티어로 운영</strong>합니다. 15분 동안 요청이 없으면
        서버가 잠들고, 다시 깨어나는 데 <strong>3~5분</strong>이 걸립니다. 새로고침하지 않아도 준비되면
        자동으로 이어집니다.
      </p>

      <div className="wakeup__bar" aria-hidden="true">
        <span style={{ width: `${progress}%` }} />
      </div>

      {overdue && (
        <p className="wakeup__desc wakeup__desc--sub">
          기다리기 어려우시면{' '}
          <a href={REPO_URL} target="_blank" rel="noreferrer">
            저장소
          </a>
          의 문서(설계·트러블슈팅·성능 측정)와 테스트 코드로 구현 내용을 바로 확인하실 수 있습니다.
        </p>
      )}
    </section>
  )
}
