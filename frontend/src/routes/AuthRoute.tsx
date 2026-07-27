import { Navigate, Outlet } from 'react-router-dom'
import { RouteSkeleton } from '@/components/RouteSkeleton.tsx'
import { useAuth } from '@/features/auth/context/AuthContext.tsx'

// Inverse of ProtectedRoute: an already-authenticated user has no reason to
// see the sign-in/sign-up forms again.
export function AuthRoute() {
  const { isAuthenticated, isInitializing } = useAuth()

  if (isInitializing) {
    return <RouteSkeleton />
  }

  if (isAuthenticated) {
    return <Navigate to="/overview" replace />
  }

  return <Outlet />
}
