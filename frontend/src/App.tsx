import { useEffect, useState } from 'react'
import { getHealth } from './api/client'

type Status = 'checking' | 'up' | 'down'

function App() {
  const [status, setStatus] = useState<Status>('checking')
  const [timestamp, setTimestamp] = useState<string | null>(null)

  useEffect(() => {
    getHealth()
      .then((data) => {
        setStatus('up')
        setTimestamp(data.timestamp)
      })
      .catch(() => setStatus('down'))
  }, [])

  return (
    <main className="app">
      <h1>스터디룸 예약 시스템</h1>
      <p className={`status status--${status}`}>
        {status === 'checking' && '백엔드 상태 확인 중...'}
        {status === 'up' && `백엔드 정상 동작 중 (${timestamp})`}
        {status === 'down' && '백엔드 연결 실패 — 서버가 실행 중인지 확인하세요.'}
      </p>
    </main>
  )
}

export default App
