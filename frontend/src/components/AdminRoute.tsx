import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

/** 로그인 + ADMIN 권한이 있어야 들어갈 수 있다. 그 외 회원은 대시보드로 돌려보낸다. */
export function AdminRoute({ children }: { children: JSX.Element }) {
  const { isAdmin } = useAuth()

  return <ProtectedRoute>{isAdmin ? children : <Navigate to="/" replace />}</ProtectedRoute>
}
