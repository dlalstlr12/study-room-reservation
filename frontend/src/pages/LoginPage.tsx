import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, Field, Input } from '../components/ui'

interface LocationState {
  from?: { pathname: string }
  email?: string
}

export function LoginPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as LocationState

  const [email, setEmail] = useState(state.email ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    setSubmitting(true)
    try {
      await login(email, password)
      toast.success('로그인되었습니다.')
      navigate(state.from?.pathname ?? '/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const fillDemo = () => {
    setEmail('admin@studyroom.local')
    setPassword('admin1234')
  }

  return (
    <div className="page page--narrow">
      <div className="page__head">
        <h1>로그인</h1>
      </div>
      <Card>
        <form className="form" onSubmit={onSubmit} noValidate>
          <Field label="이메일" htmlFor="email">
            <Input
              id="email"
              type="email"
              value={email}
              autoComplete="email"
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="비밀번호" htmlFor="password">
            <Input
              id="password"
              type="password"
              value={password}
              autoComplete="current-password"
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
          {error && <p className="form__error">{error}</p>}
          <Button type="submit" loading={submitting}>
            로그인
          </Button>
          <p className="form__foot">
            계정이 없나요? <Link to="/signup">회원가입</Link>
          </p>
        </form>
      </Card>
      <Card title="데모 관리자 계정">
        <p className="mono">admin@studyroom.local / admin1234</p>
        <Button variant="secondary" onClick={fillDemo}>
          이 계정으로 채우기
        </Button>
      </Card>
    </div>
  )
}
