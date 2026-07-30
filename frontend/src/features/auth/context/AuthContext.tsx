import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { configureAuth } from '@/lib/api/httpClient'
import { authApi } from '../api/authApi'

interface AuthContextValue {
  accessToken: string | null
  isAuthenticated: boolean
  isInitializing: boolean
  setSession: (accessToken: string, expiresIn: number) => void
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// Refresh this long before the access token actually expires, so a slow
// request or minor clock drift never races an already-dead token. Jittered
// per tab: refresh tokens rotate on every use, so tabs sharing a login (whose
// timers would otherwise target the same instant) racing to refresh
// simultaneously would have one lose against an already-revoked token — see
// docs/adr/0002-refresh-jitter-fail-soft.md.
const REFRESH_BUFFER_MS = 60_000
const REFRESH_JITTER_MS = 15_000

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(null)
  const [isInitializing, setIsInitializing] = useState(true)

  const accessTokenRef = useRef<string | null>(null)
  const expiresAtRef = useRef<number | null>(null)
  const refreshTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const refreshInFlightRef = useRef<ReturnType<typeof authApi.refresh> | null>(null)

  const clearRefreshTimer = useCallback(() => {
    if (refreshTimeoutRef.current !== null) {
      clearTimeout(refreshTimeoutRef.current)
      refreshTimeoutRef.current = null
    }
  }, [])

  const setSession = useCallback((token: string, expiresIn: number) => {
    accessTokenRef.current = token
    expiresAtRef.current = Date.now() + expiresIn * 1000
    setAccessTokenState(token)
  }, [])

  // Synchronous, network-free state clear — not exposed via the context
  // value. Used internally as httpClient's reactive "refresh failed"
  // handler, so it must never itself make an API call (that could re-trigger
  // the same 401-then-refresh path). The user-facing action is `signOut`.
  const endLocalSession = useCallback(() => {
    clearRefreshTimer()
    accessTokenRef.current = null
    expiresAtRef.current = null
    setAccessTokenState(null)
  }, [clearRefreshTimer])

  // User-initiated sign-out: revokes the refresh token server-side first
  // (best-effort — the session ends locally either way).
  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // Best-effort — proceed to clear local state regardless.
    } finally {
      endLocalSession()
    }
  }, [endLocalSession])

  // Single-flight: the bootstrap check, the proactive timer, and httpClient's
  // reactive 401 handler can all want a refresh around the same moment. They
  // share one in-flight call instead of racing separate ones, since the
  // refresh token rotates on every use and a second concurrent call would
  // just fail against the first one's already-revoked token.
  const refreshSession = useCallback(async (): Promise<string> => {
    if (!refreshInFlightRef.current) {
      refreshInFlightRef.current = authApi.refresh().finally(() => {
        refreshInFlightRef.current = null
      })
    }
    const response = await refreshInFlightRef.current
    setSession(response.accessToken, response.expiresIn)
    return response.accessToken
  }, [setSession])

  // Proactive refresh: scheduled shortly before the current access token
  // expires, so the app rarely has to fall back to httpClient's reactive
  // 401-then-refresh path. A failure here (e.g. a sibling tab already
  // rotated the refresh token) is treated as "sit tight," not "session
  // over" — it's silently skipped rather than logging the user out. Only a
  // failure of the reactive path ends the session.
  useEffect(() => {
    clearRefreshTimer()
    if (accessToken === null || expiresAtRef.current === null) {
      return
    }

    const jitter = (Math.random() * 2 - 1) * REFRESH_JITTER_MS
    const delay = Math.max(0, expiresAtRef.current - Date.now() - REFRESH_BUFFER_MS + jitter)

    refreshTimeoutRef.current = setTimeout(() => {
      refreshSession().catch(() => {
        // Fail-soft — see comment above.
      })
    }, delay)

    return clearRefreshTimer
  }, [accessToken, clearRefreshTimer, refreshSession])

  // Session bootstrap: a hard reload drops the in-memory access token, so on
  // mount we try to silently redeem the httpOnly refresh cookie (if any) for
  // a fresh one before deciding whether the user is logged in.
  useEffect(() => {
    let cancelled = false

    refreshSession()
      .catch(() => {
        // No valid refresh cookie — the user simply isn't logged in.
      })
      .finally(() => {
        if (!cancelled) setIsInitializing(false)
      })

    return () => {
      cancelled = true
    }
  }, [refreshSession])

  useEffect(() => {
    configureAuth({
      getAccessToken: () => accessTokenRef.current,
      refresh: refreshSession,
      logout: endLocalSession,
    })
  }, [endLocalSession, refreshSession])

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      isAuthenticated: accessToken !== null,
      isInitializing,
      setSession,
      signOut,
    }),
    [accessToken, isInitializing, setSession, signOut],
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
