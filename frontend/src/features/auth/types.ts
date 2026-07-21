export interface RegisterPayload {
  email: string
  username: string
  password: string
}

export interface AuthResponse {
  accessToken: string
}
