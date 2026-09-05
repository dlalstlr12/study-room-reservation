import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
  createLotteryEvent,
  deleteLotteryEvent,
  drawLotteryEvent,
  listLotteryEvents,
} from '../api/lottery'
import type { LotteryEventCreateBody } from '../api/lottery'
import { sendAnnouncement } from '../api/notifications'
import { rebuildRankings } from '../api/rankings'
import { runBilling } from '../api/subscriptions'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Field, Input, Select, Spinner, Textarea } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { LotteryAudience, LotteryEvent } from '../types'

const AUDIENCE_LABEL: Record<LotteryAudience, string> = {
  CURRENT_USERS: '현재 이용중인 회원',
  ALL_USERS: '전체 회원',
}

export function AdminPage() {
  return (
    <div className="page">
      <div className="page__head">
        <h1>관리자</h1>
        <p className="page__lead">
          일반 회원 화면에는 보이지 않는 운영 기능을 모아뒀습니다.
        </p>
      </div>

      <LotteryAdminCard />
      <AnnouncementCard />
      <RankingAdminCard />
      <BillingAdminCard />
    </div>
  )
}

function LotteryAdminCard() {
  const toast = useToast()
  const events = useApi<LotteryEvent[]>(() => listLotteryEvents(), [])
  const [creating, setCreating] = useState(false)

  const handleCreate = async (body: LotteryEventCreateBody) => {
    await createLotteryEvent(body)
    toast.success('추첨 이벤트를 만들었습니다.')
    setCreating(false)
    events.reload()
  }

  const handleDraw = async (event: LotteryEvent) => {
    try {
      await drawLotteryEvent(event.id)
      toast.success('추첨했습니다.')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '추첨에 실패했습니다.')
    }
    events.reload()
  }

  const handleDelete = async (event: LotteryEvent) => {
    if (!window.confirm(`"${event.title}" 이벤트를 삭제할까요?`)) return
    try {
      await deleteLotteryEvent(event.id)
      toast.success('이벤트를 삭제했습니다.')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '삭제에 실패했습니다.')
    }
    events.reload()
  }

  return (
    <Card
      title="이벤트 추첨 관리"
      actions={
        <Button variant={creating ? 'ghost' : 'secondary'} onClick={() => setCreating((v) => !v)}>
          {creating ? '닫기' : '새 이벤트'}
        </Button>
      }
    >
      {creating && (
        <>
          <LotteryForm onSubmit={handleCreate} />
          <div className="admin-divider" />
        </>
      )}

      {events.loading && <Spinner />}
      {events.data && events.data.length === 0 && <EmptyState>추첨 이벤트가 없습니다.</EmptyState>}
      {events.data && events.data.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>제목</th>
              <th>상태</th>
              <th>대상</th>
              <th>응모 / 당첨</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {events.data.map((event) => {
              const scheduled = event.status === 'SCHEDULED'
              return (
                <tr key={event.id}>
                  <td>{event.title}</td>
                  <td>
                    <span className={`badge badge--${scheduled ? 'reserved' : 'done'}`}>{event.status}</span>
                  </td>
                  <td className="mono-sm">{AUDIENCE_LABEL[event.audience]}</td>
                  <td className="mono-sm">
                    {event.entryCount}명 / {event.winnerCount}명
                  </td>
                  <td className="table__actions">
                    {scheduled && <Button onClick={() => handleDraw(event)}>지금 추첨</Button>}
                    <Button variant="danger" onClick={() => handleDelete(event)}>
                      삭제
                    </Button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </Card>
  )
}

function LotteryForm({ onSubmit }: { onSubmit: (body: LotteryEventCreateBody) => Promise<void> }) {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [prize, setPrize] = useState('')
  const [audience, setAudience] = useState<LotteryAudience>('CURRENT_USERS')
  const [winnerCount, setWinnerCount] = useState('1')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    if (!title.trim() || !prize.trim()) {
      setError('제목과 상품을 입력하세요.')
      return
    }
    const count = Number(winnerCount)
    if (!Number.isInteger(count) || count < 1) {
      setError('당첨 인원은 1명 이상이어야 합니다.')
      return
    }
    setSubmitting(true)
    try {
      await onSubmit({ title: title.trim(), prize: prize.trim(), audience, winnerCount: count })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '생성에 실패했습니다.')
      if (!(err instanceof ApiError)) toast.error('생성에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={submit} noValidate>
      <Field label="제목">
        <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="티타임 추첨" />
      </Field>
      <Field label="상품">
        <Input value={prize} onChange={(e) => setPrize(e.target.value)} placeholder="아메리카노 기프티콘" />
      </Field>
      <div className="form__row">
        <Field label="추첨 대상">
          <Select value={audience} onChange={(e) => setAudience(e.target.value as LotteryAudience)}>
            <option value="CURRENT_USERS">현재 이용중인 회원</option>
            <option value="ALL_USERS">전체 회원</option>
          </Select>
        </Field>
        <Field label="당첨 인원">
          <Input
            type="number"
            min={1}
            value={winnerCount}
            onChange={(e) => setWinnerCount(e.target.value)}
          />
        </Field>
      </div>
      {error && <p className="form__error">{error}</p>}
      <Button type="submit" loading={submitting}>
        만들기
      </Button>
    </form>
  )
}

function AnnouncementCard() {
  const toast = useToast()
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [open, setOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    if (!title.trim() || !body.trim()) {
      setError('제목과 내용을 입력하세요.')
      return
    }
    setSubmitting(true)
    try {
      await sendAnnouncement({ title: title.trim(), body: body.trim() })
      toast.success('전체 공지를 발송했습니다. 워커가 순차 처리합니다.')
      setTitle('')
      setBody('')
      setOpen(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '발송에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card
      title="전체 공지 보내기"
      actions={
        <Button variant={open ? 'ghost' : 'secondary'} onClick={() => setOpen((v) => !v)}>
          {open ? '닫기' : '새 공지'}
        </Button>
      }
    >
      {open ? (
        <form className="form" onSubmit={submit} noValidate>
          <Field label="제목">
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="서비스 점검 안내" />
          </Field>
          <Field label="내용">
            <Textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={3}
              placeholder="9/10 02:00~03:00 점검이 예정되어 있습니다."
            />
          </Field>
          {error && <p className="form__error">{error}</p>}
          <Button type="submit" loading={submitting}>
            모든 회원에게 발송
          </Button>
        </form>
      ) : (
        <p className="muted">모든 회원에게 알림을 비동기로 발행합니다.</p>
      )}
    </Card>
  )
}

function RankingAdminCard() {
  const toast = useToast()
  const [running, setRunning] = useState(false)

  const handleRebuild = async () => {
    setRunning(true)
    try {
      await rebuildRankings()
      toast.success('랭킹을 재구축했습니다.')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '재구축에 실패했습니다.')
    } finally {
      setRunning(false)
    }
  }

  return (
    <Card title="랭킹 재구축">
      <p className="muted">Redis 랭킹이 유실됐을 때 usage_logs 합계로 다시 만듭니다.</p>
      <Button variant="secondary" loading={running} onClick={handleRebuild}>
        재구축
      </Button>
    </Card>
  )
}

function BillingAdminCard() {
  const toast = useToast()
  const [running, setRunning] = useState(false)

  const handleRunBilling = async () => {
    setRunning(true)
    try {
      const result = await runBilling()
      toast.success(`정기결제 배치 실행: ${result.status}`)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '배치 실행에 실패했습니다.')
    } finally {
      setRunning(false)
    }
  }

  return (
    <Card title="정기결제 배치">
      <p className="muted">결제일이 도래한 PRO 구독을 지금 결제 처리합니다 (Spring Batch).</p>
      <Button variant="secondary" loading={running} onClick={handleRunBilling}>
        정기결제 실행
      </Button>
    </Card>
  )
}
