import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { createRoom, deleteRoom, listRooms, updateRoom } from '../api/rooms'
import type { RoomCreateBody } from '../api/rooms'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Input, Spinner, Textarea } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Room } from '../types'

export function RoomsPage() {
  const { isAdmin, status: authStatus } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const { data, loading, error, reload } = useApi<Room[]>(() => listRooms(), [])
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  const handleCreate = async (body: RoomCreateBody) => {
    await createRoom(body)
    toast.success('룸을 추가했습니다.')
    setCreating(false)
    reload()
  }

  const handleUpdate = async (id: number, body: RoomCreateBody) => {
    await updateRoom(id, body)
    toast.success('룸을 수정했습니다.')
    setEditingId(null)
    reload()
  }

  const handleDelete = async (room: Room) => {
    if (!window.confirm(`"${room.name}" 룸을 삭제할까요?`)) return
    try {
      await deleteRoom(room.id)
      toast.success('룸을 삭제했습니다.')
      reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '삭제에 실패했습니다.')
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <h1>룸</h1>
        <p className="page__lead">
          룸을 눌러 예약 현황을 보고 홀딩하세요. 생성·수정·삭제는 ADMIN 권한이 필요합니다.
        </p>
      </div>

      {isAdmin && (
        <Card
          title="룸 추가"
          actions={
            <Button variant={creating ? 'ghost' : 'secondary'} onClick={() => setCreating((v) => !v)}>
              {creating ? '닫기' : '새 룸'}
            </Button>
          }
        >
          {creating ? (
            <RoomForm submitLabel="추가" onSubmit={handleCreate} />
          ) : (
            <p className="muted">"새 룸" 버튼으로 폼을 엽니다.</p>
          )}
        </Card>
      )}

      {loading && <Spinner />}
      {error && <EmptyState>{error}</EmptyState>}

      {data && (
        <div className="grid grid--cards">
          {data.length === 0 && <EmptyState>등록된 룸이 없습니다.</EmptyState>}
          {data.map((room) =>
            editingId === room.id ? (
              <Card key={room.id} title={`수정 · ${room.name}`}>
                <RoomForm
                  initial={room}
                  submitLabel="저장"
                  onSubmit={(body) => handleUpdate(room.id, body)}
                  onCancel={() => setEditingId(null)}
                />
              </Card>
            ) : (
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
                  {authStatus !== 'authenticated' && (
                    <span className="muted">로그인 후 홀딩 가능</span>
                  )}
                  {isAdmin && (
                    <>
                      <Button variant="secondary" onClick={() => setEditingId(room.id)}>
                        수정
                      </Button>
                      <Button variant="danger" onClick={() => handleDelete(room)}>
                        삭제
                      </Button>
                    </>
                  )}
                </footer>
              </article>
            ),
          )}
        </div>
      )}
    </div>
  )
}

interface RoomFormProps {
  initial?: Room
  submitLabel: string
  onSubmit: (body: RoomCreateBody) => Promise<void>
  onCancel?: () => void
}

function RoomForm({ initial, submitLabel, onSubmit, onCancel }: RoomFormProps) {
  const toast = useToast()
  const [name, setName] = useState(initial?.name ?? '')
  const [capacity, setCapacity] = useState(String(initial?.capacity ?? 4))
  const [description, setDescription] = useState(initial?.description ?? '')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const cap = Number(capacity)
    const next: Record<string, string> = {}
    if (!name.trim() || name.length > 100) next.name = '이름은 1~100자입니다.'
    if (!Number.isInteger(cap) || cap < 1 || cap > 100) next.capacity = '정원은 1~100 사이 정수입니다.'
    setErrors(next)
    if (Object.keys(next).length) return

    setSubmitting(true)
    try {
      await onSubmit({ name: name.trim(), capacity: cap, description: description.trim() })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length) {
        setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.reason])))
      } else {
        toast.error(err instanceof ApiError ? err.message : '저장에 실패했습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={submit} noValidate>
      <Field label="이름" error={errors.name}>
        <Input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field label="정원" error={errors.capacity}>
        <Input
          type="number"
          min={1}
          max={100}
          value={capacity}
          onChange={(e) => setCapacity(e.target.value)}
        />
      </Field>
      <Field label="설명" error={errors.description}>
        <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>
      <div className="form__row">
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
        {onCancel && (
          <Button type="button" variant="ghost" onClick={onCancel}>
            취소
          </Button>
        )}
      </div>
    </form>
  )
}
