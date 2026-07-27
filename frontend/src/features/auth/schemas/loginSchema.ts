import { z } from 'zod'

// LoginRequestDTO has no bean validation on the backend — `login` accepts
// either an email or a username, so it can't be format-checked, and password
// rules (length, etc.) are register-time constraints, not truths about a
// password that may already exist. This schema only exists to stop an
// empty-form submit, not to second-guess the server.
export const loginSchema = z.object({
  login: z.string().trim().min(1, 'Enter your email or username'),
  password: z.string().min(1, 'Enter your password'),
})

export type LoginFormValues = z.infer<typeof loginSchema>
