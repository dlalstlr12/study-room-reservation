import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/ToastContext'
import { Button, Card, Field, Input } from '../components/ui'

export function SignupPage() {
  const { signup } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  const validate = (): boolean => {
    const next: Record<string, string> = {}
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) next.email = '올바른 이메일 형식이 아닙니다.'
    if (password.length < 8 || password.length > 64) next.password = '비밀번호는 8~64자입니다.'
    if (!name.trim() || name.length > 50) next.name = '이름은 1~50자입니다.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setSubmitting(true)
    try {
      await signup({ email, password, name })
      toast.success('회원가입 완료! 로그인해 주세요.')
      navigate('/login', { state: { email } })
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.fieldErrors.length) {
          setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.reason])))
        } else {
          toast.error(err.message)
        }
      } else {
        toast.error('회원가입에 실패했습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page page--narrow">
      <div className="page__head">
        <h1>회원가입</h1>
      </div>
      <Card>
        <form className="form" onSubmit={onSubmit} noValidate>
          <Field label="이메일" htmlFor="email" error={errors.email}>
            <Input
              id="email"
              type="email"
              value={email}
              autoComplete="email"
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="비밀번호" htmlFor="password" error={errors.password} hint="8~64자">
            <Input
              id="password"
              type="password"
              value={password}
              autoComplete="new-password"
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
          <Field label="이름" htmlFor="name" error={errors.name}>
            <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Button type="submit" loading={submitting}>
            가입하기
          </Button>
          <p className="form__foot">
            이미 계정이 있나요? <Link to="/login">로그인</Link>
          </p>
        </form>
      </Card>
    </div>
  )
}
