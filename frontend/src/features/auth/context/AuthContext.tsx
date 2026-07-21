import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

interface AuthContextValue {
  accessToken: string | null
  isAuthenticated: boolean
  setAccessToken: (token: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// Access token lives in memory only (never localStorage/sessionStorage) to keep it
// out of reach of any XSS-readable storage — the API pairs this with an httpOnly
// refresh cookie for longer-lived sessions. This does mean a hard page reload
// currently drops the in-memory token; wiring a silent refresh-on-load belongs
// with the future login/session-bootstrap work.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(null)

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      isAuthenticated: accessToken !== null,
      setAccessToken: (token: string) => setAccessTokenState(token),
      logout: () => setAccessTokenState(null),
    }),
    [accessToken],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
