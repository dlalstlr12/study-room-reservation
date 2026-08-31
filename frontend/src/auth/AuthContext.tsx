import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { getMe, login as loginRequest, logout as logoutRequest, signUp } from '../api/auth'
import type { SignUpBody } from '../api/auth'
import type { Member } from '../types'
import { AUTH_EXPIRED_EVENT, clearTokens, getAccessToken, setTokens } from './tokenStore'

interface AuthContextValue {
  user: Member | null
  status: 'loading' | 'authenticated' | 'anonymous'
  isAdmin: boolean
  login: (email: string, password: string) => Promise<void>
  signup: (body: SignUpBody) => Promise<Member>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Member | null>(null)
  const [status, setStatus] = useState<AuthContextValue['status']>('loading')

  const resetToAnonymous = useCallback(() => {
    clearTokens()
    setUser(null)
    setStatus('anonymous')
  }, [])

  // 새로고침 시 저장된 토큰으로 사용자 복원
  useEffect(() => {
    if (!getAccessToken()) {
      setStatus('anonymous')
      return
    }
    getMe()
      .then((me) => {
        setUser(me)
        setStatus('authenticated')
      })
      .catch(() => resetToAnonymous())
  }, [resetToAnonymous])

  // client.ts가 재발급 실패 시 발생시키는 이벤트 구독
  useEffect(() => {
    const handler = () => resetToAnonymous()
    window.addEventListener(AUTH_EXPIRED_EVENT, handler)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler)
  }, [resetToAnonymous])

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await loginRequest({ email, password })
    setTokens(tokens)
    const me = await getMe()
    setUser(me)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } catch {
      /* 이미 만료됐어도 로컬 정리는 진행 */
    }
    resetToAnonymous()
  }, [resetToAnonymous])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAdmin: user?.role === 'ADMIN',
      login,
      signup: signUp,
      logout,
    }),
    [user, status, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
