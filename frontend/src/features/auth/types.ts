export interface RegisterPayload {
  email: string
  username: string
  password: string
}

export interface LoginPayload {
  login: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  expiresIn: number
}
