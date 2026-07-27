const API_BASE_URL = '/api/v1'

export class ApiError extends Error {
  readonly status: number
  readonly errors: string[]

  constructor(status: number, errors: string[]) {
    super(errors[0] ?? 'Something went wrong. Please try again.')
    this.status = status
    this.errors = errors
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /**
   * Marks this call as requiring a session: attaches `Authorization: Bearer`,
   * and on a 401 refreshes the session once and retries once before giving up.
   * Leave unset for public endpoints (login/register/refresh themselves) —
   * a 401 there is a normal, user-facing error, not an expired session.
   */
  authenticated?: boolean
}

interface AuthHandlers {
  getAccessToken: () => string | null
  refresh: () => Promise<string>
  logout: () => void
}

let authHandlers: AuthHandlers | null = null

/** Wires AuthProvider's session state into apiRequest. Call once, near app startup. */
export function configureAuth(handlers: AuthHandlers) {
  authHandlers = handlers
}

let refreshInFlight: Promise<string> | null = null

function refreshOnce(): Promise<string> {
  if (!authHandlers) {
    return Promise.reject(new Error('configureAuth must be called before an authenticated request'))
  }
  if (!refreshInFlight) {
    refreshInFlight = authHandlers.refresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

function send(path: string, options: RequestOptions, accessToken: string | null): Promise<Response> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

  return fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    credentials: 'include',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
}

/**
 * Thin fetch wrapper for the Spring API: same-origin relative paths (works
 * through the Vite proxy in dev and will work through nginx in prod),
 * `credentials: 'include'` so the httpOnly refresh cookie round-trips, and it
 * normalizes GlobalExceptionHandler's `{ errors: string[] }` response shape
 * into a typed ApiError. Feature-level API modules call this instead of
 * `fetch` directly.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = options.authenticated ? (authHandlers?.getAccessToken() ?? null) : null
  let response = await send(path, options, accessToken)

  if (options.authenticated && response.status === 401 && authHandlers) {
    try {
      const newAccessToken = await refreshOnce()
      response = await send(path, options, newAccessToken)
      if (response.status === 401) {
        authHandlers.logout()
      }
    } catch {
      authHandlers.logout()
    }
  }

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const errors: string[] =
      Array.isArray(data?.errors) && data.errors.length > 0
        ? data.errors
        : ['Something went wrong. Please try again.']
    throw new ApiError(response.status, errors)
  }

  return data as T
}
