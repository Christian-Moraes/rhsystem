import api from './api'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: {
    id: string
    name: string
    email: string
  }
  companies: Array<{
    id: string
    name: string
    role: string
  }>
}

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', { email, password })
    return response.data
  },

  async register(name: string, email: string, password: string): Promise<void> {
    await api.post('/auth/register', { name, email, password })
  },
}
