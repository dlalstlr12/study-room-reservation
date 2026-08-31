import type { TokenResponse } from '../types'

const ACCESS_KEY = 'sr_access'
const REFRESH_KEY = 'sr_refresh'

export function getAccessToken(): string | null {
  try {
    return localStorage.getItem(ACCESS_KEY)
  } catch {
    return null
  }
}

export function getRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY)
  } catch {
    return null
  }
}

export function setTokens(tokens: Pick<TokenResponse, 'accessToken' | 'refreshToken'>): void {
  try {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken)
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  } catch {
    /* localStorage 사용 불가 환경은 무시 */
  }
}

export function clearTokens(): void {
  try {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  } catch {
    /* noop */
  }
}

/** 세션 만료(재발급 실패) 시 앱 전역에 알린다. AuthContext가 구독한다. */
export const AUTH_EXPIRED_EVENT = 'sr:auth-expired'

export function notifyAuthExpired(): void {
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
}
