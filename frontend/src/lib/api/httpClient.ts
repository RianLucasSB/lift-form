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
}

/**
 * Thin fetch wrapper for the Spring API: same-origin (Vite proxies /api in dev,
 * nginx will in prod), sends cookies for the httpOnly refresh token, and
 * normalizes GlobalExceptionHandler's `{ errors: string[] }` shape into ApiError.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers: options.body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    credentials: 'include',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

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
