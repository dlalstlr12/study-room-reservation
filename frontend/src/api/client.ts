import type { ErrorResponse, FieldError, TokenResponse } from '../types'
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  notifyAuthExpired,
  setTokens,
} from '../auth/tokenStore'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  code: string
  status: number
  fieldErrors: FieldError[]

  constructor(status: number, body: Partial<ErrorResponse> | null) {
    super(body?.message ?? `요청이 실패했습니다 (${status})`)
    this.name = 'ApiError'
    this.status = status
    this.code = body?.code ?? 'UNKNOWN'
    this.fieldErrors = body?.fieldErrors ?? []
  }

  fieldError(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.reason
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** true면 access 토큰을 붙이지 않는다 (로그인/회원가입 등) */
  anonymous?: boolean
  query?: Record<string, string | number | undefined>
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(API_BASE_URL + path)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function parseBody(res: Response): Promise<unknown> {
  if (res.status === 204) return undefined
  const text = await res.text()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function tryReissue(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  const res = await fetch(buildUrl('/api/auth/reissue'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!res.ok) return false
  const tokens = (await res.json()) as TokenResponse
  setTokens(tokens)
  return true
}

async function rawRequest<T>(path: string, options: RequestOptions, retrying: boolean): Promise<T> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (!options.anonymous) {
    const token = getAccessToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (res.status === 401 && !options.anonymous && !retrying) {
    const reissued = await tryReissue()
    if (reissued) return rawRequest<T>(path, options, true)
    clearTokens()
    notifyAuthExpired()
  }

  const body = await parseBody(res)
  if (!res.ok) {
    throw new ApiError(res.status, (body ?? null) as Partial<ErrorResponse> | null)
  }
  return body as T
}

export function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return rawRequest<T>(path, options, false)
}
