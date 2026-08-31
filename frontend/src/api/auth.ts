import type { Member, TokenResponse } from '../types'
import { apiFetch } from './client'

export interface SignUpBody {
  email: string
  password: string
  name: string
}

export interface LoginBody {
  email: string
  password: string
}

export function signUp(body: SignUpBody): Promise<Member> {
  return apiFetch<Member>('/api/auth/signup', { method: 'POST', body, anonymous: true })
}

export function login(body: LoginBody): Promise<TokenResponse> {
  return apiFetch<TokenResponse>('/api/auth/login', { method: 'POST', body, anonymous: true })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}

export function getMe(): Promise<Member> {
  return apiFetch<Member>('/api/members/me')
}
