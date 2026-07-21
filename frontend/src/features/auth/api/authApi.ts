import { apiRequest } from '@/lib/api/httpClient'
import type { AuthResponse, RegisterPayload } from '../types'

export const authApi = {
  register(payload: RegisterPayload) {
    return apiRequest<AuthResponse>('/auth/register', { method: 'POST', body: payload })
  },
}
