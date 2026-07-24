import { apiRequest } from '@/lib/api/httpClient'
import type { AuthResponse, LoginPayload, RegisterPayload } from '../types'

export const authApi = {
  register(payload: RegisterPayload) {
    return apiRequest<AuthResponse>('/auth/register', { method: 'POST', body: payload })
  },
  login(payload: LoginPayload) {
    return apiRequest<AuthResponse>('/auth/login', { method: 'POST', body: payload })
  },
  refresh() {
    return apiRequest<AuthResponse>('/auth/refresh', { method: 'POST' })
  },
}
