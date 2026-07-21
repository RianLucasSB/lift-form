import { useState } from 'react'
import { ApiError } from '@/lib/api/httpClient'
import { authApi } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import type { RegisterPayload } from '../types'

export function useRegister() {
  const { setAccessToken } = useAuth()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function register(payload: RegisterPayload): Promise<boolean> {
    setIsSubmitting(true)
    setError(null)
    try {
      const { accessToken } = await authApi.register(payload)
      setAccessToken(accessToken)
      return true
    } catch (err) {
      setError(err instanceof ApiError ? err.errors.join(' ') : 'Something went wrong. Please try again.')
      return false
    } finally {
      setIsSubmitting(false)
    }
  }

  return { register, isSubmitting, error }
}
