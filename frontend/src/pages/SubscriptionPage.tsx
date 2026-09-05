import { ApiError } from '../api/client'
import { cancelSubscription, getMySubscription, listMyPayments, subscribePro } from '../api/subscriptions'
import { useToast } from '../components/ToastContext'
import { Button, Card, EmptyState, Spinner } from '../components/ui'
import { useApi } from '../hooks/useApi'
import type { Payment, Subscription } from '../types'
import { formatDate } from '../utils/format'

const STATUS_LABEL: Record<NonNullable<Subscription['status']>, string> = {
  ACTIVE: '이용 중',
  PAST_DUE: '결제 실패',
  CANCELLED: '해지됨',
}

const won = (n: number) => `${n.toLocaleString('ko-KR')}원`

export function SubscriptionPage() {
  const toast = useToast()
  const sub = useApi<Subscription>(() => getMySubscription(), [])
  const payments = useApi<Payment[]>(() => listMyPayments(), [])

  const reload = () => {
    sub.reload()
    payments.reload()
  }

  const handleSubscribe = async () => {
    try {
      await subscribePro()
      toast.success('PRO 구독을 시작했습니다. 다음 정기결제일에 첫 결제가 진행됩니다.')
      reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '구독에 실패했습니다.')
    }
  }

  const handleCancel = async () => {
    if (!window.confirm('구독을 해지할까요? 홀딩 연장 혜택이 사라집니다.')) return
    try {
      await cancelSubscription()
      toast.info('구독을 해지했습니다.')
      reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '해지에 실패했습니다.')
    }
  }

  const plan = sub.data?.plan ?? 'FREE'
  const status = sub.data?.status ?? null
  const isPro = plan === 'PRO' && status !== 'CANCELLED'

  return (
    <div className="page">
      <div className="page__head">
        <h1>구독</h1>
        <p className="page__lead">
          PRO 구독자는 좌석 <strong>홀딩 유예가 20분</strong>으로 늘어납니다(기본 10분). 정기결제는 매일
          자정 <strong>Spring Batch</strong>가 처리하고, 결제·상태변경·이벤트 발행은 트랜잭션 아웃박스로
          묶여 유실되지 않습니다.
        </p>
      </div>

      <Card title="내 플랜">
        {sub.loading && <Spinner />}
        {sub.data && (
          <div className="plan">
            <div className="plan__badge">
              <span className={`plan__name plan__name--${plan.toLowerCase()}`}>{plan}</span>
              {status && (
                <span
                  className={`badge badge--${status === 'ACTIVE' ? 'free' : status === 'PAST_DUE' ? 'stop' : 'done'}`}
                >
                  {STATUS_LABEL[status]}
                </span>
              )}
            </div>
            <dl className="plan__meta">
              {isPro && (
                <>
                  <div>
                    <dt>월 요금</dt>
                    <dd className="mono-sm">{won(sub.data.priceKrw)}</dd>
                  </div>
                  <div>
                    <dt>다음 결제일</dt>
                    <dd className="mono-sm">
                      {sub.data.nextBillingAt ? formatDate(sub.data.nextBillingAt) : '-'}
                    </dd>
                  </div>
                </>
              )}
              <div>
                <dt>홀딩 유예</dt>
                <dd className="mono-sm">{isPro && status === 'ACTIVE' ? '20분' : '10분'}</dd>
              </div>
            </dl>
            <div className="plan__actions">
              {isPro ? (
                <Button variant="danger" onClick={handleCancel}>
                  해지
                </Button>
              ) : (
                <Button onClick={handleSubscribe}>PRO 구독하기 · {won(9900)}/월</Button>
              )}
            </div>
          </div>
        )}
      </Card>

      <Card title="결제 이력">
        {payments.loading && <Spinner />}
        {payments.data && payments.data.length === 0 && (
          <EmptyState>결제 이력이 없습니다.</EmptyState>
        )}
        {payments.data && payments.data.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>일시</th>
                <th>금액</th>
                <th>상태</th>
                <th>사유</th>
              </tr>
            </thead>
            <tbody>
              {payments.data.map((p) => (
                <tr key={p.id}>
                  <td className="table__time">{formatDate(p.paidAt)}</td>
                  <td className="mono-sm">{won(p.amountKrw)}</td>
                  <td>
                    <span className={`badge badge--${p.status === 'SUCCEEDED' ? 'free' : 'stop'}`}>
                      {p.status === 'SUCCEEDED' ? '성공' : '실패'}
                    </span>
                  </td>
                  <td className="muted">{p.failureReason ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  )
}
