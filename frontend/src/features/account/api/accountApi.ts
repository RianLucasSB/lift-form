import { apiRequest } from '@/lib/api/httpClient'
import type { CurrentUser } from '../types'

export const accountApi = {
  getCurrentUser() {
    return apiRequest<CurrentUser>('/users/me', { authenticated: true })
  },
}
