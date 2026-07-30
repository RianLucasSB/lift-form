import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useAuth } from '@/features/auth/context/AuthContext'
import { accountApi } from '../api/accountApi'
import type { CurrentUser } from '../types'

interface AccountContextValue {
  currentUser: CurrentUser | null
  isLoading: boolean
}

const AccountContext = createContext<AccountContextValue | undefined>(undefined)

// Populated once per session (on login/bootstrap), not on every proactive
// token refresh — this effect only re-runs when isAuthenticated flips, not
// when the access token itself rotates.
export function AccountProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) {
      setCurrentUser(null)
      return
    }

    let cancelled = false
    setIsLoading(true)

    accountApi
      .getCurrentUser()
      .then((user) => {
        if (!cancelled) setCurrentUser(user)
      })
      .catch(() => {
        if (!cancelled) setCurrentUser(null)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [isAuthenticated])

  return <AccountContext.Provider value={{ currentUser, isLoading }}>{children}</AccountContext.Provider>
}

export function useAccount() {
  const context = useContext(AccountContext)
  if (!context) {
    throw new Error('useAccount must be used within an AccountProvider')
  }
  return context
}
