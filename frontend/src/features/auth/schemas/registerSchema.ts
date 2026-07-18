import { z } from 'zod'

// Mirrors RegisterRequestDTO's bean validation (@NotEmpty/@Email on email,
// @NotEmpty on username, @NotEmpty @Size(min=8, max=72) on password) so the
// form fails the same inputs the API would reject before a request is ever sent.
export const registerSchema = z
  .object({
    email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
    username: z.string().trim().min(1, 'Username is required'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be at most 72 characters'),
    confirmPassword: z.string().min(1, 'Confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
