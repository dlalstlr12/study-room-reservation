import { useNavigate } from 'react-router-dom'
import { listRooms } from '../api/rooms'
import { useAuth } from '../auth/AuthContext'
import { Button, EmptyState, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Room } from '../types'

export function RoomsPage() {
  const { status: authStatus } = useAuth()
  const navigate = useNavigate()

  const { data, loading, error } = useApi<Room[]>(() => listRooms(), [])

  return (
    <div className="page">
      <div className="page__head">
        <h1>룸</h1>
        <p className="page__lead">룸을 눌러 예약 현황을 보고 홀딩하세요.</p>
      </div>

      {loading && <Spinner />}
      {error && <EmptyState>{error}</EmptyState>}

      {data && (
        <div className="grid grid--cards">
          {data.length === 0 && <EmptyState>등록된 룸이 없습니다.</EmptyState>}
          {data.map((room) => (
            <article
              key={room.id}
              className="room-card room-card--link"
              role="button"
              tabIndex={0}
              onClick={() => navigate(`/rooms/${room.id}`)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') navigate(`/rooms/${room.id}`)
              }}
            >
              <header className="room-card__head">
                <h3>{room.name}</h3>
                <span className="room-card__id">#{room.id}</span>
              </header>
              <p className="room-card__meta">정원 {room.capacity}명</p>
              {room.description && <p className="room-card__desc">{room.description}</p>}
              <footer className="room-card__foot" onClick={(e) => e.stopPropagation()}>
                <Button variant="primary" onClick={() => navigate(`/rooms/${room.id}`)}>
                  예약 현황
                </Button>
                {authStatus !== 'authenticated' && <span className="muted">로그인 후 홀딩 가능</span>}
              </footer>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
