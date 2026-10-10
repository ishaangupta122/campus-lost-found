import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { LoadingState } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'

/**
 * Wraps pages that need a login. role="STUDENT" or role="ADMIN" also requires that role
 * (admins cannot report items or claim them; students cannot see the admin area).
 * This only hides pages in the UI; the backend is what really enforces access.
 */
export default function ProtectedRoute({ role }) {
  const { user, isAuthenticated, initializing } = useAuth()
  const location = useLocation()

  if (initializing) return <LoadingState message="Checking your session..." />

  if (!isAuthenticated) {
    // Remember where the user wanted to go, so login can send them back.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (role && user.role !== role) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />
  }

  return <Outlet />
}
