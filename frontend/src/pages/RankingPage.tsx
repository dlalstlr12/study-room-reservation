import { useState } from 'react'
import { ApiError } from '../api/client'
import { getMyRank, getRankings, rebuildRankings } from '../api/rankings'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { MyRank, RankingEntry, RankingScope } from '../types'
import { formatMinutes } from '../utils/format'

const SCOPES: { value: RankingScope; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'daily', label: '오늘' },
]

const MEDAL = ['🥇', '🥈', '🥉']

export function RankingPage() {
  const { user, isAdmin } = useAuth()
  const toast = useToast()
  const [scope, setScope] = useState<RankingScope>('all')

  const rankings = useApi<RankingEntry[]>(() => getRankings(scope), [scope])
  const myRank = useApi<MyRank>(() => getMyRank(scope), [scope])

  const handleRebuild = async () => {
    try {
      await rebuildRankings()
      toast.success('랭킹을 재구축했습니다.')
      rankings.reload()
      myRank.reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '재구축에 실패했습니다.')
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <h1>이용시간 랭킹</h1>
        <p className="page__lead">
          퇴실하면 실제 이용한 시간이 <strong>Redis Sorted Set</strong>에 누적됩니다. 조회는 DB 집계
          없이 <code>ZREVRANGE</code>로 바로 옵니다.
        </p>
      </div>

      <div className="rank-tabs">
        {SCOPES.map((s) => (
          <button
            key={s.value}
            type="button"
            className={`rank-tab${scope === s.value ? ' is-active' : ''}`}
            onClick={() => setScope(s.value)}
          >
            {s.label}
          </button>
        ))}
      </div>

      {myRank.data && (
        <div className="rank-me">
          <span className="rank-me__label">내 순위</span>
          <span className="rank-me__value">
            {myRank.data.rank ? `${myRank.data.rank}위` : '기록 없음'}
          </span>
          <span className="rank-me__minutes">{formatMinutes(myRank.data.minutes)}</span>
        </div>
      )}

      <Card
        title={scope === 'all' ? '전체 누적' : '오늘'}
        actions={
          isAdmin ? (
            <Button variant="ghost" onClick={handleRebuild}>
              재구축
            </Button>
          ) : undefined
        }
      >
        {rankings.loading && <Spinner />}
        {rankings.error && <EmptyState>{rankings.error}</EmptyState>}
        {rankings.data && rankings.data.length === 0 && (
          <EmptyState>아직 퇴실 기록이 없습니다.</EmptyState>
        )}
        {rankings.data && rankings.data.length > 0 && (
          <ol className="rank-list">
            {rankings.data.map((entry) => (
              <li
                key={entry.memberId}
                className={`rank-row${entry.rank <= 3 ? ` rank-row--top rank-row--${entry.rank}` : ''}${
                  user && entry.memberId === user.id ? ' rank-row--me' : ''
                }`}
              >
                <span className="rank-row__rank">{MEDAL[entry.rank - 1] ?? entry.rank}</span>
                <span className="rank-row__name">{entry.memberName}</span>
                <span className="rank-row__minutes">{formatMinutes(entry.minutes)}</span>
              </li>
            ))}
          </ol>
        )}
      </Card>
    </div>
  )
}
