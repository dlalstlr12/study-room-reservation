import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Spinner } from './ui'

export function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <Spinner label="세션 확인 중…" />
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return children
}
