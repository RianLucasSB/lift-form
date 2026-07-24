import { useState } from 'react'
import { ApiError } from '@/lib/api/httpClient'
import { authApi } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import type { LoginPayload } from '../types'

export function useLogin() {
  const { setSession } = useAuth()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function login(payload: LoginPayload): Promise<boolean> {
    setIsSubmitting(true)
    setError(null)
    try {
      const { accessToken, expiresIn } = await authApi.login(payload)
      setSession(accessToken, expiresIn)
      return true
    } catch (err) {
      setError(err instanceof ApiError ? err.errors.join(' ') : 'Something went wrong. Please try again.')
      return false
    } finally {
      setIsSubmitting(false)
    }
  }

  return { login, isSubmitting, error }
}
